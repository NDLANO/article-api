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
import com.sksamuel.elastic4s.searches.queries.BoolQuery
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties.{
  ConceptSearchIndex,
  ElasticSearchIndexMaxResultWindow,
  ElasticSearchScrollKeepAlive
}
import no.ndla.articleapi.integration.Elastic4sClient
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.{ConceptSummary, ResultWindowTooLargeException}
import no.ndla.articleapi.model.domain.Language.getSupportedLanguages
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.search.{SearchResult, SearchableConcept, SearchableLanguageFormats}
import no.ndla.articleapi.service.ConverterService
import org.json4s._
import org.json4s.native.Serialization.read

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait ConceptSearchService {
  this: Elastic4sClient with SearchService with ConceptIndexService with ConverterService =>
  val conceptSearchService: ConceptSearchService

  class ConceptSearchService extends LazyLogging with SearchService[api.ConceptSummary] {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
    override val searchIndex: String = ConceptSearchIndex

    override def hitToApiModel(hitString: String, language: String): api.ConceptSummary = {
      val searchableConcept = read[SearchableConcept](hitString)

      val titles = searchableConcept.title.languageValues.map(lv => ConceptTitle(lv.value, lv.language))
      val contents = searchableConcept.content.languageValues.map(lv => ConceptContent(lv.value, lv.language))

      val supportedLanguages = getSupportedLanguages(titles, contents)

      val title = Language
        .findByLanguageOrBestEffort(titles, language)
        .map(converterService.toApiConceptTitle)
        .getOrElse(api.ConceptTitle("", Language.UnknownLanguage))
      val concept = Language
        .findByLanguageOrBestEffort(contents, language)
        .map(converterService.toApiConceptContent)
        .getOrElse(api.ConceptContent("", Language.UnknownLanguage))

      api.ConceptSummary(
        searchableConcept.id,
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
            fallback: Boolean): Try[SearchResult[ConceptSummary]] = {
      executeSearch(withIdIn, language, sort, page, pageSize, boolQuery(), fallback)
    }

    def matchingQuery(query: String,
                      withIdIn: List[Long],
                      searchLanguage: String,
                      page: Int,
                      pageSize: Int,
                      sort: Sort.Value,
                      fallback: Boolean): Try[SearchResult[ConceptSummary]] = {
      val language = if (searchLanguage == Language.AllLanguages) "*" else searchLanguage
      val titleSearch = simpleStringQuery(query).field(s"title.$language", 2)
      val contentSearch = simpleStringQuery(query).field(s"content.$language", 1)

      val fullQuery = boolQuery()
        .must(
          boolQuery()
            .should(
              titleSearch,
              contentSearch
            )
        )

      executeSearch(withIdIn, language, sort, page, pageSize, fullQuery, fallback)
    }

    def executeSearch(withIdIn: List[Long],
                      language: String,
                      sort: Sort.Value,
                      page: Int,
                      pageSize: Int,
                      queryBuilder: BoolQuery,
                      fallback: Boolean): Try[SearchResult[ConceptSummary]] = {

      val idFilter = if (withIdIn.isEmpty) None else Some(idsQuery(withIdIn))

      val (languageFilter, searchLanguage) = language match {
        case Language.AllLanguages | "*" =>
          (None, "*")
        case lang =>
          if (fallback)
            (None, "*")
          else
            (Some(existsQuery(s"title.$lang")), lang)
      }

      val filters = List(idFilter, languageFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = pageSize * page
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow")
        Failure(ResultWindowTooLargeException())
      } else {

        val searchToExecute = search(searchIndex)
          .size(numResults)
          .from(startAt)
          .query(filteredSearch)
          .sortBy(getSortDefinition(sort, searchLanguage))
          .highlighting(highlight("*"))

        // Only add scroll param if it is first page
        val searchWithScroll =
          if (startAt != 0) { searchToExecute } else { searchToExecute.scroll(ElasticSearchScrollKeepAlive) }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            Success(
              SearchResult(
                response.result.totalHits,
                Some(page),
                numResults,
                if (language == "*") Language.AllLanguages else language,
                getHits(response.result, language, fallback),
                response.result.scrollId
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
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

  }

}
