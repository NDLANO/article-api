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
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.ElasticClientComponent
import no.ndla.articleapi.model.ArticleInformation
import org.json4s.native.Serialization.write

trait ElasticContentIndexComponent {
  this: ElasticClientComponent =>
  val elasticContentIndex: ElasticContentIndex

  class ElasticContentIndex extends LazyLogging {

    def indexDocuments(articleData: List[ArticleInformation], indexName: String): Int = {
      implicit val formats = org.json4s.DefaultFormats

      elasticClient.execute {
        bulk(articleData.map(content => {
          index into indexName -> ArticleApiProperties.SearchDocument source write(content) id content.id
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
        createIndex(indexName) mappings (
          ArticleApiProperties.SearchDocument as(
            "id" typed IntegerType,
            "titles" typed NestedType as(
              "title" typed StringType,
              "language" typed StringType index "not_analyzed"
              ),
            "article" typed NestedType as(
              "article" typed StringType analyzer "HtmlAnalyzer",
              "language" typed StringType index "not_analyzed"
              ),
            "copyright" typed NestedType as(
              "license" typed NestedType as(
                "license" typed StringType index "not_analyzed",
                "description" typed StringType,
                "url" typed StringType
                ),
              "origin" typed StringType,
              "authors" typed NestedType as(
                "type" typed StringType,
                "name" typed StringType
                )
              ),
            "tags" typed NestedType as(
              "tags" typed StringType,
              "language" typed StringType index "not_analyzed"
              ),
            "requiredLibraries" typed NestedType as(
              "mediaType" typed StringType index "not_analyzed",
              "name" typed StringType index "not_analyzed",
              "url" typed StringType index "not_analyzed"
              )
            )
          ) analysis CustomAnalyzerDefinition("HtmlAnalyzer", StandardTokenizer, StandardTokenFilter, LowercaseTokenFilter, HtmlStripCharFilter)
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
  }

}
