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

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.mappings.{MappingContentBuilder, NestedFieldDefinition}
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Bulk, Delete, Index}
import io.searchbox.indices.aliases.{AddAliasMapping, GetAliases, ModifyAliases, RemoveAliasMapping}
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.{CreateIndex, DeleteIndex, IndicesExists}
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.ElasticClient
import no.ndla.articleapi.model.domain.Language.languageAnalyzers
import no.ndla.articleapi.model.domain.{Article, NdlaSearchException}
import no.ndla.articleapi.model.search.SearchableLanguageFormats
import org.elasticsearch.ElasticsearchException
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: ElasticClient with SearchConverterService =>
  val indexService: IndexService

  class IndexService extends LazyLogging {

    private def createIndexRequest(article: Article, indexName: String) = {
      implicit val formats = SearchableLanguageFormats.JSonFormats
      val source = write(searchConverterService.asSearchableArticle(article))
      new Index.Builder(source).index(indexName).`type`(ArticleApiProperties.SearchDocument).id(article.id.get.toString).build
    }

    def indexDocument(article: Article): Try[Article] = {
      val result = jestClient.execute(createIndexRequest(article, ArticleApiProperties.SearchIndex))
      result.map(_ => article)
    }

    def indexDocuments(articles: List[Article], indexName: String): Try[Int] = {
      val bulkBuilder = new Bulk.Builder()
      articles.foreach(article => bulkBuilder.addAction(createIndexRequest(article, indexName)))

      val response = jestClient.execute(bulkBuilder.build())
      response.map(r => {
        logger.info(s"Indexed ${articles.size} documents. No of failed items: ${r.getFailedItems.size()}")
        articles.size
      })
    }

    def deleteDocument(articleId: Long): Try[_] = {
      for {
        _ <- indexService.aliasTarget.map {
          case Some(index) => Success(index)
          case None => createIndex().map(newIndex => updateAliasTarget(None, newIndex))
        }
        deleted <- {
          jestClient.execute(
            new Delete.Builder(s"$articleId").index(ArticleApiProperties.SearchIndex).`type`(ArticleApiProperties.SearchDocument).build()
          )
        }
      } yield deleted
    }

    def createIndex(): Try[String] = {
      createIndexWithName(ArticleApiProperties.SearchIndex + "_" + getTimestamp)
    }

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val createIndexResponse = jestClient.execute(new CreateIndex.Builder(indexName).build())
        createIndexResponse.map(_ => createMapping(indexName)).map(_ => indexName)
      }
    }

    def createMapping(indexName: String): Try[String] = {
      val mappingResponse = jestClient.execute(new PutMapping.Builder(indexName, ArticleApiProperties.SearchDocument, buildMapping()).build())
      mappingResponse.map(_ => indexName)
    }

    def buildMapping() = {
      MappingContentBuilder.buildWithName(mapping(ArticleApiProperties.SearchDocument).fields(
        intField("id"),
        languageSupportedField("title", keepRaw = true),
        languageSupportedField("content"),
        languageSupportedField("visualElement"),
        languageSupportedField("introduction"),
        languageSupportedField("tags"),
        dateField("lastUpdated"),
        keywordField("license") index "not_analyzed",
        textField("authors") fielddata(true),
        textField("articleType") analyzer "keyword"
      ), ArticleApiProperties.SearchDocument).string()
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      val languageSupportedField = new NestedFieldDefinition(fieldName)
      languageSupportedField._fields = keepRaw match {
        case true => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true) analyzer langAnalyzer.analyzer fields (keywordField("raw") index "not_analyzed"))
        case false => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true) analyzer langAnalyzer.analyzer)
      }

      languageSupportedField
    }

    def aliasTarget: Try[Option[String]] = {
      val getAliasRequest = new GetAliases.Builder().addIndex(s"${ArticleApiProperties.SearchIndex}").build()
      jestClient.execute(getAliasRequest) match {
        case Success(result) => {
          val aliasIterator = result.getJsonObject.entrySet().iterator()
          aliasIterator.hasNext match {
            case true => Success(Some(aliasIterator.next().getKey))
            case false => Success(None)
          }
        }
        case Failure(_: NdlaSearchException) => Success(None)
        case Failure(t: Throwable) => Failure(t)
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        val addAliasDefinition = new AddAliasMapping.Builder(newIndexName, ArticleApiProperties.SearchIndex).build()
        val modifyAliasRequest = oldIndexName match {
          case None => new ModifyAliases.Builder(addAliasDefinition).build()
          case Some(oldIndex) => {
            new ModifyAliases.Builder(
              new RemoveAliasMapping.Builder(oldIndex, ArticleApiProperties.SearchIndex).build()
            ).addAlias(addAliasDefinition).build()
          }
        }

        jestClient.execute(modifyAliasRequest)
      }
    }

    def deleteIndex(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success()
        case Some(indexName) => {
          if (!indexExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            jestClient.execute(new DeleteIndex.Builder(indexName).build())
          }
        }
      }
    }

    def indexExists(indexName: String): Try[Boolean] = {
      jestClient.execute(new IndicesExists.Builder(indexName).build()) match {
        case Success(_) => Success(true)
        case Failure(_: ElasticsearchException) => Success(false)
        case Failure(t: Throwable) => Failure(t)
      }
    }

    def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }
}
