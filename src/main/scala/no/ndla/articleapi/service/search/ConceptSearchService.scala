/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import com.google.gson.JsonObject
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.Search
import io.searchbox.params.Parameters
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.ElasticClient
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.domain.{Concept, Language, NdlaSearchException, Sort}
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.elasticsearch.search.builder.SearchSourceBuilder

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait ConceptSearchService {
  this: ElasticClient with SearchService with ConceptIndexService =>
  val conceptSearchService: ConceptSearchService

  class ConceptSearchService extends LazyLogging with SearchService[api.Concept] {
    override val searchIndex: String = ArticleApiProperties.ConceptSearchIndex

    private def getSearchLanguage(supportedLanguages: Seq[String], language: String): String = {
      language match {
        case Language.NoLanguage if supportedLanguages.contains(Language.DefaultLanguage) => Language.DefaultLanguage
        case Language.NoLanguage if supportedLanguages.nonEmpty => supportedLanguages.head
        case lang => lang
      }
    }

    override def hitToApiModel(hit: JsonObject, language: String): api.Concept = {
      val titles = hit.get("title").getAsJsonObject.entrySet.asScala.to[Seq]
      val concepts = hit.get("content").getAsJsonObject.entrySet.asScala.to[Seq]
      val supportedLanguages = titles.map(_.getKey).union(concepts.map(_.getKey)).distinct
      val searchLanguage: String = getSearchLanguage(supportedLanguages, language)

      val title = titles.find(_.getKey == searchLanguage).map(_.getValue.getAsString).getOrElse("")
      val concept = concepts.find(_.getKey == searchLanguage).map(_.getValue.getAsString).getOrElse("")

      api.Concept(
        hit.get("id").getAsLong,
        title,
        concept.toString,
        searchLanguage,
        supportedLanguages
      )
    }

    def all(withIdIn: List[Long], language: String, page: Int, pageSize: Int, sort: Sort.Value): api.ConceptSearchResult = {
      executeSearch(withIdIn, language, sort, page, pageSize, QueryBuilders.boolQuery())
    }

    def matchingQuery(query: String, withIdIn: List[Long], searchLanguage: String, page: Int, pageSize: Int, sort: Sort.Value): api.ConceptSearchResult = {
      val titleSearch = QueryBuilders.simpleQueryStringQuery(query).field(s"title.$searchLanguage")
      val contentSearch = QueryBuilders.simpleQueryStringQuery(query).field(s"content.$searchLanguage")

      val fullQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.boolQuery()
          .should(QueryBuilders.nestedQuery("title", titleSearch, ScoreMode.Avg).boost(2))
          .should(QueryBuilders.nestedQuery("content", contentSearch, ScoreMode.Avg).boost(1)))

      executeSearch(withIdIn, searchLanguage, sort, page, pageSize, fullQuery)
    }

    def executeSearch(withIdIn: List[Long], language: String, sort: Sort.Value, page: Int, pageSize: Int, queryBuilder: BoolQueryBuilder): api.ConceptSearchResult = {
      val idFilteredSearch = withIdIn.nonEmpty match {
        case true => queryBuilder.filter(QueryBuilders.idsQuery(ArticleApiProperties.ConceptSearchDocument).addIds(withIdIn.map(_.toString):_*))
        case false => queryBuilder
      }
      val searchQuery = new SearchSourceBuilder().query(idFilteredSearch).sort(getSortDefinition(sort, language))

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val request = new Search.Builder(searchQuery.toString)
        .addIndex(searchIndex)
        .setParameter(Parameters.SIZE, numResults) .setParameter("from", startAt)


      jestClient.execute(request.build()) match {
        case Success(response) => api.ConceptSearchResult(response.getTotal.toLong, page, numResults, getHits(response, language))
        case Failure(f) => errorHandler(Failure(f))
      }
    }

    protected def errorHandler[T](failure: Failure[T]) = {
      failure match {
        case Failure(e: NdlaSearchException) =>
          e.getResponse.getResponseCode match {
            case notFound: Int if notFound == 404 =>
              logger.error(s"Index $searchIndex not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              throw new IndexNotFoundException(s"Index $searchIndex not found. Scheduling a reindex")
            case _ =>
              logger.error(e.getResponse.getErrorMessage)
              throw new ElasticsearchException(s"Unable to execute search in $searchIndex", e.getResponse.getErrorMessage)
          }
        case Failure(t: Throwable) => throw t
      }
    }

    private def scheduleIndexDocuments() = {
      val f = Future {
        conceptIndexService.indexDocuments
      }

      f onFailure { case t => logger.warn("Unable to create index: " + t.getMessage, t) }
      f onSuccess {
        case Success(reindexResult) => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

  }
}
