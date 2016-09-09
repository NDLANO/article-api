/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.ElasticClientComponent
import no.ndla.articleapi.model.search.{SearchableArticle, SearchableArticleInformation}
import no.ndla.articleapi.model.{ArticleSummary, SearchResult, Sort}
import no.ndla.network.ApplicationUrl
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.transport.RemoteTransportException

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.json4s.native.Serialization.read

trait SearchService {
  this: ElasticClientComponent with SearchIndexServiceComponent with SearchConverterService =>
  val searchService: SearchService

  class SearchService extends LazyLogging {

    val noCopyrightFilter = not(termQuery("license", "copyrighted"))

    implicit object ContentHitAs extends HitAs[ArticleSummary] {
      override def as(hit: RichSearchHit): ArticleSummary = {
        implicit val formats = org.json4s.DefaultFormats
        searchConverterService.asArticleSummary(read[SearchableArticleInformation](hit.sourceAsString))
      }
    }

    def all(language: Option[String], license: Option[String], page: Option[Int], pageSize: Option[Int], sort: Sort.Value): SearchResult = {
      val searchLanguage = language.getOrElse(ArticleApiProperties.DefaultLanguage)
      val filterList = new ListBuffer[QueryDefinition]()
      license.foreach(license => filterList += termQuery("license", license))
      filterList += noCopyrightFilter

      val theSearch = search in ArticleApiProperties.SearchIndex -> ArticleApiProperties.SearchDocument query filter(filterList)

      executeSearch(theSearch, searchLanguage, sort, page, pageSize)
    }

    def matchingQuery(query: Iterable[String], language: Option[String], license: Option[String], page: Option[Int], pageSize: Option[Int], sort: Sort.Value): SearchResult = {
      val searchLanguage = language.getOrElse(ArticleApiProperties.DefaultLanguage)

      val titleSearch = matchQuery(s"titles.$searchLanguage", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      val articleSearch = matchQuery(s"article.$searchLanguage", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      val tagSearch = matchQuery(s"tags.$searchLanguage", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)

      val filterList = new ListBuffer[QueryDefinition]()
      license.foreach(license => filterList += termQuery("license", license))
      filterList += noCopyrightFilter

      val theSearch = search in ArticleApiProperties.SearchIndex -> ArticleApiProperties.SearchDocument query {
        bool {
          must(
            should(
              nestedQuery("titles").query(titleSearch),
              nestedQuery("article").query(articleSearch),
              nestedQuery("tags").query(tagSearch)
            ),
            filter (filterList)
          )
        }
      }
      executeSearch(theSearch, searchLanguage, sort, page, pageSize)
    }

    def getSortDefinition(sort: Sort.Value, language: String): SortDefinition = {
      sort match {
        case (Sort.ByTitleAsc) => fieldSort(s"titles.$language.raw").nestedPath("titles").order(SortOrder.ASC).missing("_last")
        case (Sort.ByTitleDesc) => fieldSort(s"titles.$language.raw").nestedPath("titles").order(SortOrder.DESC).missing("_last")
        case (Sort.ByRelevanceAsc) => fieldSort("_score").order(SortOrder.ASC)
        case (Sort.ByRelevanceDesc) => fieldSort("_score").order(SortOrder.DESC)
      }
    }

    def executeSearch(search: SearchDefinition, language: String, sort: Sort.Value, page: Option[Int], pageSize: Option[Int]): SearchResult = {
      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      try {
        val actualSearch = search.sort(getSortDefinition(sort, language)).start(startAt).limit(numResults)
        val response = elasticClient.execute {
          actualSearch
        }.await

        SearchResult(response.getHits.getTotalHits, page.getOrElse(1), numResults, response.as[ArticleSummary])
      } catch {
        case e: RemoteTransportException => errorHandler(e.getCause)
      }
    }

    def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
      val numResults = pageSize match {
        case Some(num) =>
          if (num > 0) num.min(ArticleApiProperties.MaxPageSize) else ArticleApiProperties.DefaultPageSize
        case None => ArticleApiProperties.DefaultPageSize
      }

      val startAt = page match {
        case Some(sa) => (sa - 1).max(0) * numResults
        case None => 0
      }

      (startAt, numResults)
    }

    def errorHandler(exception: Throwable) = {
      exception match {
        case ex: IndexNotFoundException =>
          logger.error(ex.getDetailedMessage)
          scheduleIndexDocuments()
          throw ex
        case _ => throw exception
      }
    }

    def scheduleIndexDocuments() = {
      val f = Future {
        searchIndexService.indexDocuments()
      }
      f onFailure { case t => logger.error("Unable to create index: " + t.getMessage) }
    }
  }

}
