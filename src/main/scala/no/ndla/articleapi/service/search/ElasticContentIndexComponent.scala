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
import org.json4s.native.Serialization.write

trait ElasticContentIndexComponent {
  this: ElasticClientComponent with SearchConverterService =>
  val elasticContentIndex: ElasticContentIndex

  class ElasticContentIndex extends LazyLogging {
    val langToAnalyzer = Map(
      NORWEGIAN_BOKMAL -> NorwegianLanguageAnalyzer,
      NORWEGIAN_NYNORSK -> NorwegianLanguageAnalyzer,
      ENGLISH -> EnglishLanguageAnalyzer,
      FRENCH -> FrenchLanguageAnalyzer,
      GERMAN -> GermanLanguageAnalyzer,
      SPANISH -> SpanishLanguageAnalyzer,
      SAMI -> StandardAnalyzer,
      CHINESE -> ChineseLanguageAnalyzer,
      UNKNOWN -> StandardAnalyzer
    )

    def indexDocuments(articleData: List[ArticleInformation], indexName: String): Int = {
      implicit val formats = org.json4s.DefaultFormats

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
      elasticClient.execute {
        createIndex(indexName)
          .mappings(mapping(ArticleApiProperties.SearchDocument).fields(
            "id" typed IntegerType,
            languageSupportedField("titles", keepRaw = true),
            languageSupportedField("article", keepRaw = false),
            languageSupportedField("tags", keepRaw = false),
            "license" typed StringType index "not_analyzed",
            "authors" typed StringType
          ))
      }.await
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
      if (keepRaw) {
        new NestedFieldDefinition(fieldName).as(
          NORWEGIAN_BOKMAL typed StringType analyzer langToAnalyzer(NORWEGIAN_BOKMAL) fields ("raw" typed StringType index "not_analyzed"),
          NORWEGIAN_NYNORSK typed StringType analyzer langToAnalyzer(NORWEGIAN_NYNORSK) fields ("raw" typed StringType index "not_analyzed"),
          ENGLISH typed StringType analyzer langToAnalyzer(ENGLISH) fields ("raw" typed StringType index "not_analyzed"),
          FRENCH typed StringType analyzer langToAnalyzer(FRENCH) fields ("raw" typed StringType index "not_analyzed"),
          GERMAN typed StringType analyzer langToAnalyzer(GERMAN) fields ("raw" typed StringType index "not_analyzed"),
          SPANISH typed StringType analyzer langToAnalyzer(SPANISH) fields ("raw" typed StringType index "not_analyzed"),
          SAMI typed StringType analyzer langToAnalyzer(SAMI) fields ("raw" typed StringType index "not_analyzed"),
          CHINESE typed StringType analyzer langToAnalyzer(CHINESE) fields ("raw" typed StringType index "not_analyzed"),
          UNKNOWN typed StringType analyzer langToAnalyzer(UNKNOWN) fields ("raw" typed StringType index "not_analyzed")
        )
      } else {
        new NestedFieldDefinition(fieldName).as(
          NORWEGIAN_BOKMAL typed StringType analyzer langToAnalyzer(NORWEGIAN_BOKMAL),
          NORWEGIAN_NYNORSK typed StringType analyzer langToAnalyzer(NORWEGIAN_NYNORSK),
          ENGLISH typed StringType analyzer langToAnalyzer(ENGLISH),
          FRENCH typed StringType analyzer langToAnalyzer(FRENCH),
          GERMAN typed StringType analyzer langToAnalyzer(GERMAN),
          SPANISH typed StringType analyzer langToAnalyzer(SPANISH),
          SAMI typed StringType analyzer langToAnalyzer(SAMI),
          CHINESE typed StringType analyzer langToAnalyzer(CHINESE),
          UNKNOWN typed StringType analyzer langToAnalyzer(UNKNOWN)
        )
      }
    }
  }

}
