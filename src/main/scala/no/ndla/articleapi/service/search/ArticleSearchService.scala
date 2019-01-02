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
  ArticleSearchIndex,
  ElasticSearchIndexMaxResultWindow,
  ElasticSearchScrollKeepAlive
}
import no.ndla.articleapi.integration.Elastic4sClient
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.{ArticleSummaryV2, ResultWindowTooLargeException, SearchResultV2}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.search.SearchResult
import no.ndla.articleapi.service.ConverterService
import no.ndla.mapping.License

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait ArticleSearchService {
  this: Elastic4sClient with SearchConverterService with SearchService with ArticleIndexService with ConverterService =>
  val articleSearchService: ArticleSearchService

  class ArticleSearchService extends LazyLogging with SearchService[api.ArticleSummaryV2] {
    private val noCopyright = boolQuery().not(termQuery("license", License.Copyrighted.toString))

    override val searchIndex: String = ArticleSearchIndex

    override def hitToApiModel(hit: String, language: String): api.ArticleSummaryV2 = {
      converterService.hitAsArticleSummaryV2(hit, language)
    }

    def all(withIdIn: List[Long],
            language: String,
            license: Option[String],
            page: Int,
            pageSize: Int,
            sort: Sort.Value,
            articleTypes: Seq[String],
            fallback: Boolean): Try[SearchResult[ArticleSummaryV2]] = {
      executeSearch(withIdIn, language, license, sort, page, pageSize, boolQuery(), articleTypes, fallback)
    }

    def matchingQuery(query: String,
                      withIdIn: List[Long],
                      searchLanguage: String,
                      license: Option[String],
                      page: Int,
                      pageSize: Int,
                      sort: Sort.Value,
                      articleTypes: Seq[String],
                      fallback: Boolean): Try[SearchResult[ArticleSummaryV2]] = {
      val language = if (searchLanguage == Language.AllLanguages || fallback) "*" else searchLanguage
      val titleSearch = simpleStringQuery(query).field(s"title.$language", 2)
      val introSearch = simpleStringQuery(query).field(s"introduction.$language", 2)
      val metaSearch = simpleStringQuery(query).field(s"metaDescription.$language", 1)
      val contentSearch = simpleStringQuery(query).field(s"content.$language", 1)
      val tagSearch = simpleStringQuery(query).field(s"tags.$language", 1)

      val fullQuery = boolQuery()
        .must(
          boolQuery()
            .should(
              titleSearch,
              introSearch,
              metaSearch,
              contentSearch,
              tagSearch
            )
        )

      executeSearch(withIdIn, searchLanguage, license, sort, page, pageSize, fullQuery, articleTypes, fallback)
    }

    def executeSearch(withIdIn: List[Long],
                      language: String,
                      license: Option[String],
                      sort: Sort.Value,
                      page: Int,
                      pageSize: Int,
                      queryBuilder: BoolQuery,
                      articleTypes: Seq[String],
                      fallback: Boolean): Try[SearchResult[ArticleSummaryV2]] = {

      val articleTypesFilter =
        if (articleTypes.nonEmpty) Some(constantScoreQuery(termsQuery("articleType", articleTypes))) else None
      val idFilter = if (withIdIn.isEmpty) None else Some(idsQuery(withIdIn))

      val licenseFilter = license match {
        case None      => Some(noCopyright)
        case Some(lic) => Some(termQuery("license", lic))
      }

      val (languageFilter, searchLanguage) = language match {
        case "" | Language.AllLanguages =>
          (None, "*")
        case lang =>
          if (fallback)
            (None, "*")
          else (Some(existsQuery(s"title.$lang")), lang)
      }

      val filters = List(licenseFilter, idFilter, languageFilter, articleTypesFilter)
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
          .highlighting(highlight("*"))
          .sortBy(getSortDefinition(sort, searchLanguage))

        // Only add scroll param if it is first page
        val searchWithScroll =
          if (startAt != 0) { searchToExecute } else { searchToExecute.scroll(ElasticSearchScrollKeepAlive) }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            Success(
              SearchResult[ArticleSummaryV2](
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
        articleIndexService.indexDocuments
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
