/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import java.util.Map.Entry

import com.google.gson.{JsonElement, JsonObject, JsonParser}
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.Search
import io.searchbox.params.Parameters
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.{Elastic4sClient, ElasticClient}
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.ResultWindowTooLargeException
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.service.ConverterService
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.ScoreMode
import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.{BoolQueryBuilder, InnerHitBuilder, QueryBuilders}
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait ConceptSearchService {
  this: ElasticClient with Elastic4sClient with SearchService with ConceptIndexService with ConverterService =>
  val conceptSearchService: ConceptSearchService

  class ConceptSearchService extends LazyLogging with SearchService[api.ConceptSummary] {
    override val searchIndex: String = ArticleApiProperties.ConceptSearchIndex

    private def getSearchLanguage(supportedLanguages: Seq[String], language: String): String = {
      language match {
        case Language.NoLanguage if supportedLanguages.contains(Language.DefaultLanguage) => Language.DefaultLanguage
        case Language.NoLanguage if supportedLanguages.nonEmpty => supportedLanguages.head
        case lang => lang
      }
    }

    override def hitToApiModel(hitString: String, language: String): api.ConceptSummary = {
      val hit = new JsonParser().parse(hitString).getAsJsonObject
      val titles = getEntrySetSeq(hit, "title").map(ent => ConceptTitle(ent.getValue.getAsString, ent.getKey))
      val contents = getEntrySetSeq(hit, "content").map(ent => ConceptContent(ent.getValue.getAsString, ent.getKey))
      val supportedLanguages = (titles union contents).map(_.language).toSet

      val title = Language.findByLanguageOrBestEffort(titles, language).map(converterService.toApiConceptTitle).getOrElse(api.ConceptTitle("", Language.DefaultLanguage))
      val concept = Language.findByLanguageOrBestEffort(contents, language).map(converterService.toApiConceptContent).getOrElse(api.ConceptContent("", Language.DefaultLanguage))

      api.ConceptSummary(
        hit.get("id").getAsLong,
        title,
        concept,
        supportedLanguages
      )
    }

    def getEntrySetSeq(hit: JsonObject, fieldPath: String): Seq[Entry[String, JsonElement]] = {
      hit.get(fieldPath).getAsJsonObject.entrySet.asScala.to[Seq]
    }

    def all(withIdIn: List[Long], language: String, page: Int, pageSize: Int, sort: Sort.Value): api.ConceptSearchResult = {
      executeSearch(withIdIn, language, sort, page, pageSize, boolQuery())
    }

    def matchingQuery(query: String, withIdIn: List[Long], searchLanguage: String, page: Int, pageSize: Int, sort: Sort.Value): api.ConceptSearchResult = {
      val language = if (searchLanguage == Language.AllLanguages) "*" else searchLanguage
      val titleSearch = simpleStringQuery(query).field(s"title.$language")
      val contentSearch = simpleStringQuery(query).field(s"content.$language")

      val hi = highlight("*").preTag("").postTag("").numberOfFragments(0)
      val ih = innerHits("inner_hits").highlighting(hi)

      val fullQuery = boolQuery()
        .must(
          boolQuery()
            .should(
              nestedQuery("title", titleSearch).scoreMode(ScoreMode.Avg).inner(ih),
              nestedQuery("content", contentSearch).scoreMode(ScoreMode.Avg).inner(ih)
            )
        )


      executeSearch(withIdIn, language, sort, page, pageSize, fullQuery)
    }

    def executeSearch(withIdIn: List[Long], language: String, sort: Sort.Value, page: Int, pageSize: Int, queryBuilder: BoolQueryDefinition): api.ConceptSearchResult = {
      val searchLanguage = language match {
        case Language.AllLanguages | "*" => "*"
        case _ => language
      }

      val idFilter = if (withIdIn.isEmpty) None else Some(idsQuery(withIdIn))

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)

      val filters = List(idFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val requestedResultWindow = pageSize * page
      if (requestedResultWindow > ArticleApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${ArticleApiProperties.ElasticSearchIndexMaxResultWindow}, user requested ${requestedResultWindow}")
        throw new ResultWindowTooLargeException()
      }

      e4sClient.execute{
        search(searchIndex).size(numResults).from(startAt).query(filteredSearch).sortBy(getSortDefinition(sort, searchLanguage))
      } match {
        case Success(response) =>
          api.ConceptSearchResult(response.result.totalHits, page, numResults, getHits(response.result, language, hitToApiModel))
        case Failure(ex) =>
          errorHandler(Failure(ex))
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

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

  }
}
