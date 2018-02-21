/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import java.util.concurrent.Executors

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.ScoreMode
import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.Elastic4sClient
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.{FallbackTitleSortUnsupportedException, ResultWindowTooLargeException}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.service.ConverterService
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.{DefaultFormats, _}
import org.json4s.native.JsonMethods._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

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

      val title = Language.findByLanguageOrBestEffort(titles, language).map(converterService.toApiConceptTitle).getOrElse(api.ConceptTitle("", Language.UnknownLanguage))
      val concept = Language.findByLanguageOrBestEffort(contents, language).map(converterService.toApiConceptContent).getOrElse(api.ConceptContent("", Language.UnknownLanguage))

      api.ConceptSummary(
        (hit \ "id").extract[Long],
        title,
        concept,
        supportedLanguages
      )
    }

    def all(withIdIn: List[Long],
            language: String,
            page: Int,
            pageSize: Int,
            sort: Sort.Value,
            fallback: Boolean): Try[api.ConceptSearchResult] = {
      executeSearch(withIdIn, language, sort, page, pageSize, boolQuery(), fallback)
    }

    def matchingQuery(query: String,
                      withIdIn: List[Long],
                      searchLanguage: String,
                      page: Int,
                      pageSize: Int,
                      sort: Sort.Value,
                      fallback: Boolean): Try[api.ConceptSearchResult] = {
      val language = if (searchLanguage == Language.AllLanguages) "*" else searchLanguage
      val titleSearch = simpleStringQuery(query).field(s"title.$language", 2)
      val contentSearch = simpleStringQuery(query).field(s"content.$language", 1)

      val hi = highlight("*").preTag("").postTag("").numberOfFragments(0)

      val fullQuery = boolQuery()
        .must(
          boolQuery()
            .should(
              nestedQuery("title", titleSearch).scoreMode(ScoreMode.Avg).inner(innerHits("title").highlighting(hi)),
              nestedQuery("content", contentSearch).scoreMode(ScoreMode.Avg).inner(innerHits("content").highlighting(hi))
            )
        )


      executeSearch(withIdIn, language, sort, page, pageSize, fullQuery, fallback)
    }

    def executeSearch(withIdIn: List[Long],
                      language: String,
                      sort: Sort.Value,
                      page: Int,
                      pageSize: Int,
                      queryBuilder: BoolQueryDefinition,
                      fallback: Boolean): Try[api.ConceptSearchResult] = {

      val idFilter = if (withIdIn.isEmpty) None else Some(idsQuery(withIdIn))

      val (languageFilter, searchLanguage) = language match {
        case Language.AllLanguages | "*" =>
          (None, "*")
        case lang =>
          fallback match {
            case true => (None, "*")
            case false => (Some(nestedQuery("title", existsQuery(s"title.$lang")).scoreMode(ScoreMode.Avg)), lang)
          }
      }

      val filters = List(idFilter, languageFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = pageSize * page
      if (requestedResultWindow > ArticleApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${ArticleApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        Failure(ResultWindowTooLargeException())
      } else if (fallback && (sort == Sort.ByTitleAsc || sort == Sort.ByTitleDesc)){
        logger.info("User attempted to sort by title when using fallback parameter")
        Failure(FallbackTitleSortUnsupportedException())
      } else {
        e4sClient.execute{
          search(searchIndex).size(numResults).from(startAt).query(filteredSearch).sortBy(getSortDefinition(sort, searchLanguage))
        } match {
          case Success(response) =>
            Success(api.ConceptSearchResult(
              response.result.totalHits,
              page,
              numResults,
              if(language == "*") Language.AllLanguages else language,
              getHits(response.result, language, fallback)
            ))
          case Failure(ex) => errorHandler(ex)
        }
      }
    }

    override def scheduleIndexDocuments(): Unit = {
      implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
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
