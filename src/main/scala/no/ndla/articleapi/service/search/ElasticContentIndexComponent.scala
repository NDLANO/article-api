/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.search

import java.text.SimpleDateFormat
import java.util.Calendar

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.FieldType.{IntegerType, NestedType, StringType}
import com.sksamuel.elastic4s.mappings.NestedFieldDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.ElasticClientComponent
import no.ndla.articleapi.model.ArticleInformation
import no.ndla.articleapi.model.Language._
import no.ndla.articleapi.model.search.SearchableLanguageFormats
import org.json4s.native.Serialization.write

trait ElasticContentIndexComponent {
  this: ElasticClientComponent with SearchConverterService =>
  val elasticContentIndex: ElasticContentIndex

  class ElasticContentIndex extends LazyLogging {
    def indexDocuments(articleData: List[ArticleInformation], indexName: String): Int = {
      implicit val formats = SearchableLanguageFormats.JSonFormats

      val searchableArticles = articleData.map(searchConverterService.asSearchableArticleInformation)
      elasticClient.execute {
        bulk(searchableArticles.map(articleInformation => {
          index into indexName -> ArticleApiProperties.SearchDocument source write(articleInformation) id articleInformation.id
        }))
      }.await

      logger.info(s"Indexed ${articleData.size} documents")
      articleData.size
    }

    def create(): String = {
      val indexName = ArticleApiProperties.SearchIndex + "_" + getTimestamp

      val existsDefinition = elasticClient.execute {
        index exists indexName.toString
      }.await

      if (!existsDefinition.isExists) {
        createElasticIndex(indexName)
      }

      indexName
    }

    private def createElasticIndex(indexName: String) = {
      val createIndexTemplate = createIndex(indexName)
        .mappings(mapping(ArticleApiProperties.SearchDocument).fields(
          "id" typed IntegerType,
          languageSupportedField("titles", keepRaw = true),
          languageSupportedField("article", keepRaw = false),
          languageSupportedField("tags", keepRaw = false),
          "license" typed StringType index "not_analyzed",
          "authors" typed StringType
        ))

      elasticClient.execute(createIndexTemplate).await
    }

    def aliasTarget: Option[String] = {
      val res = elasticClient.execute {
        get alias ArticleApiProperties.SearchIndex
      }.await
      val aliases = res.getAliases.keysIt()
      aliases.hasNext match {
        case true => Some(aliases.next())
        case false => None
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Unit = {
      val existsDefinition = elasticClient.execute {
        index exists newIndexName
      }.await

      if (existsDefinition.isExists) {
        elasticClient.execute {
          oldIndexName.foreach(oldIndexName => {
            remove alias ArticleApiProperties.SearchIndex on oldIndexName

          })
          add alias ArticleApiProperties.SearchIndex on newIndexName
        }.await
      } else {
        throw new IllegalArgumentException(s"No such index: $newIndexName")
      }
    }

    def delete(indexName: String): Unit = {
      val existsDefinition = elasticClient.execute {
        index exists indexName
      }.await

      if (existsDefinition.isExists) {
        elasticClient.execute {
          deleteIndex(indexName)
        }.await
      } else {
        throw new IllegalArgumentException(s"No such index: $indexName")
      }
    }

    def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      val languageSupportedField = new NestedFieldDefinition(fieldName)
      languageSupportedField._fields = keepRaw match {
        case true => languageAnalyzers.map(langAnalyzer => langAnalyzer.lang typed StringType analyzer langAnalyzer.analyzer fields ("raw" typed StringType index "not_analyzed"))
        case false => languageAnalyzers.map(langAnalyzer => langAnalyzer.lang typed StringType analyzer langAnalyzer.analyzer)
      }

      languageSupportedField
    }
  }
}
