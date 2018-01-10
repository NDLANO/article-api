/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.ScoreMode
import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.Elastic4sClient
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.ResultWindowTooLargeException
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.service.ConverterService
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.{DefaultFormats, _}
import org.json4s.native.JsonMethods._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait ConceptSearchService {
  this: Elastic4sClient with SearchService with ConceptIndexService with ConverterService =>
  val conceptSearchService: ConceptSearchService

  class ConceptSearchService extends LazyLogging with SearchService[api.ConceptSummary] {
    implicit val formats = DefaultFormats
    override val searchIndex: String = ArticleApiProperties.ConceptSearchIndex

    override def hitToApiModel(hitString: String, language: String): api.ConceptSummary = {
      val hit = parse(hitString)
      val titles = (hit \ "title").extract[Map[String, String]].map(title => ConceptTitle(title._2, title._1)).toSeq
      val contents = (hit \ "content").extract[Map[String, String]].map(content => ConceptContent(content._2, content._1)).toSeq

      val supportedLanguages = (titles union contents).map(_.language).toSet

      val title = Language.findByLanguageOrBestEffort(titles, language).map(converterService.toApiConceptTitle).getOrElse(api.ConceptTitle("", Language.DefaultLanguage))
      val concept = Language.findByLanguageOrBestEffort(contents, language).map(converterService.toApiConceptContent).getOrElse(api.ConceptContent("", Language.DefaultLanguage))

      api.ConceptSummary(
        (hit \ "id").extract[Long],
        title,
        concept,
        supportedLanguages
      )
    }

    def all(withIdIn: List[Long], language: String, page: Int, pageSize: Int, sort: Sort.Value): api.ConceptSearchResult = {
      executeSearch(withIdIn, language, sort, page, pageSize, boolQuery())
    }

    def matchingQuery(query: String, withIdIn: List[Long], searchLanguage: String, page: Int, pageSize: Int, sort: Sort.Value): api.ConceptSearchResult = {
      val language = if (searchLanguage == Language.AllLanguages) "*" else searchLanguage
      val titleSearch = simpleStringQuery(query).field(s"title.$language", 2)
      val contentSearch = simpleStringQuery(query).field(s"content.$language", 1)

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
        logger.info(s"Max supported results are ${ArticleApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
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
          e.rf.status match {
            case notFound: Int if notFound == 404 =>
              logger.error(s"Index $searchIndex not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              throw new IndexNotFoundException(s"Index $searchIndex not found. Scheduling a reindex")
            case _ =>
              logger.error(e.getMessage)
              throw new ElasticsearchException(s"Unable to execute search in $searchIndex", e.getMessage)
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
