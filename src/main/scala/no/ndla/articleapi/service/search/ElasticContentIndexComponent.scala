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
import com.sksamuel.elastic4s.mappings.FieldType.{DateType, IntegerType, StringType}
import com.sksamuel.elastic4s.mappings.NestedFieldDefinition
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Bulk, Index}
import io.searchbox.indices.aliases.{AddAliasMapping, GetAliases, ModifyAliases, RemoveAliasMapping}
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.{CreateIndex, DeleteIndex, IndicesExists}
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.ElasticClientComponent
import no.ndla.articleapi.model.domain.Article
import no.ndla.articleapi.model.domain.Language.languageAnalyzers
import no.ndla.articleapi.model.search.SearchableLanguageFormats
import org.elasticsearch.ElasticsearchException
import org.json4s.native.Serialization.write

trait ElasticContentIndexComponent {
  this: ElasticClientComponent with SearchConverterService =>
  val elasticContentIndex: ElasticContentIndex

  class ElasticContentIndex extends LazyLogging {

    def indexDocuments(articleData: List[Article], indexName: String): Int = {
      implicit val formats = SearchableLanguageFormats.JSonFormats
      val searchableArticles = articleData.map(searchConverterService.asSearchableArticle)

      val bulkBuilder = new Bulk.Builder()
      searchableArticles.foreach(imageMeta => {
        val source = write(imageMeta)
        bulkBuilder.addAction(new Index.Builder(source).index(indexName).`type`(ArticleApiProperties.SearchDocument).id(imageMeta.id.toString).build)
      })

      val response = jestClient.execute(bulkBuilder.build())
      if (!response.isSucceeded) {
        throw new ElasticsearchException(s"Unable to index documents to ${ArticleApiProperties.SearchIndex}. ErrorMessage: {}", response.getErrorMessage)
      }
      logger.info(s"Indexed ${searchableArticles.size} documents")
      searchableArticles.size
    }

    def createIndex(): String = {
      val indexName = ArticleApiProperties.SearchIndex + "_" + getTimestamp
      if (!indexExists(indexName)) {
        val createIndexResponse = jestClient.execute(new CreateIndex.Builder(indexName).build())
        createIndexResponse.isSucceeded match {
          case false => throw new ElasticsearchException(s"Unable to create index $indexName. ErrorMessage: {}", createIndexResponse.getErrorMessage)
          case true => createMapping(indexName)
        }
      }
      indexName
    }

    def createMapping(indexName: String) = {
      val mappingResponse = jestClient.execute(new PutMapping.Builder(indexName, ArticleApiProperties.SearchDocument, buildMapping()).build())
      if (!mappingResponse.isSucceeded) {
        throw new ElasticsearchException(s"Unable to create mapping for index $indexName. ErrorMessage: {}", mappingResponse.getErrorMessage)
      }
    }

    def buildMapping(): String = {
      mapping(ArticleApiProperties.SearchDocument).fields(
        "id" typed IntegerType,
        languageSupportedField("title", keepRaw = true),
        languageSupportedField("content", keepRaw = false),
        languageSupportedField("tags", keepRaw = false),
        "lastUpdated" typed DateType,
        "license" typed StringType index "not_analyzed",
        "authors" typed StringType
      ).buildWithName.string()
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      val languageSupportedField = new NestedFieldDefinition(fieldName)
      languageSupportedField._fields = keepRaw match {
        case true => languageAnalyzers.map(langAnalyzer => langAnalyzer.lang typed StringType analyzer langAnalyzer.analyzer fields ("raw" typed StringType index "not_analyzed"))
        case false => languageAnalyzers.map(langAnalyzer => langAnalyzer.lang typed StringType analyzer langAnalyzer.analyzer)
      }

      languageSupportedField
    }

    def aliasTarget: Option[String] = {
      val getAliasRequest = new GetAliases.Builder().addIndex(s"${ArticleApiProperties.SearchIndex}").build()
      val result = jestClient.execute(getAliasRequest)
      result.isSucceeded match {
        case false => None
        case true => {
          val aliasIterator = result.getJsonObject.entrySet().iterator()
          aliasIterator.hasNext match {
            case true => Some(aliasIterator.next().getKey)
            case false => None
          }
        }
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String) = {
      if (indexExists(newIndexName)) {
        val addAliasDefinition = new AddAliasMapping.Builder(newIndexName, ArticleApiProperties.SearchIndex).build()
        val modifyAliasRequest = oldIndexName match {
          case None => new ModifyAliases.Builder(addAliasDefinition).build()
          case Some(oldIndex) => {
            new ModifyAliases.Builder(
              new RemoveAliasMapping.Builder(oldIndex, ArticleApiProperties.SearchIndex).build()
            ).addAlias(addAliasDefinition).build()
          }
        }

        val response = jestClient.execute(modifyAliasRequest)
        if (!response.isSucceeded) {
          logger.error(response.getErrorMessage)
          throw new ElasticsearchException(s"Unable to modify alias ${ArticleApiProperties.SearchIndex} -> $oldIndexName to ${ArticleApiProperties.SearchIndex} -> $newIndexName. ErrorMessage: {}", response.getErrorMessage)
        }
      } else {
        throw new IllegalArgumentException(s"No such index: $newIndexName")
      }
    }

    def delete(indexName: String) = {
      if (indexExists(indexName)) {
        val response = jestClient.execute(new DeleteIndex.Builder(indexName).build())
        if (!response.isSucceeded) {
          throw new ElasticsearchException(s"Unable to delete index $indexName. ErrorMessage: {}", response.getErrorMessage)
        }
      } else {
        throw new IllegalArgumentException(s"No such index: $indexName")
      }
    }

    def indexExists(indexName: String): Boolean = {
      jestClient.execute(new IndicesExists.Builder(indexName).build()).isSucceeded
    }

    def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }
}
