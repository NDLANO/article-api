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
import no.ndla.articleapi.model.{ArticleSummary, SearchResult}
import no.ndla.network.ApplicationUrl
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.transport.RemoteTransportException

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ElasticContentSearchComponent {
  this: ElasticClientComponent with SearchIndexServiceComponent =>
  val elasticContentSearch: ElasticContentSearch

  class ElasticContentSearch extends LazyLogging {

    val noCopyrightFilter = not(nestedQuery("copyright.license").query(termQuery("copyright.license.license", "copyrighted")))

    implicit object ContentHitAs extends HitAs[ArticleSummary] {
      override def as(hit: RichSearchHit): ArticleSummary = {
        val sourceMap = hit.sourceAsMap
        ArticleSummary(
          sourceMap("id").toString,
          sourceMap("titles").asInstanceOf[java.util.ArrayList[AnyRef]].get(0).asInstanceOf[java.util.HashMap[String, String]].get("title"),
          ApplicationUrl.get + sourceMap("id").toString,
          sourceMap("copyright").asInstanceOf[java.util.HashMap[String, AnyRef]].get("license").asInstanceOf[java.util.HashMap[String, String]].get("license"))
      }
    }

    def all(license: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResult = {
      val filterList = new ListBuffer[QueryDefinition]()
      license.foreach(license => filterList += nestedQuery("copyright.license").query(termQuery("copyright.license.license", license)))
      filterList += noCopyrightFilter

      val theSearch = search in ArticleApiProperties.SearchIndex -> ArticleApiProperties.SearchDocument query filter(filterList)
      theSearch.sort(field sort "id")

      executeSearch(theSearch, page, pageSize)
    }

    def matchingQuery(query: Iterable[String], language: Option[String], license: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResult = {
      val titleSearch = new ListBuffer[QueryDefinition]
      titleSearch += matchQuery("titles.title", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      language.foreach(lang => titleSearch += termQuery("titles.language", lang))

      val articleSearch = new ListBuffer[QueryDefinition]
      articleSearch += matchQuery("article.article", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      language.foreach(lang => articleSearch += termQuery("article.language", lang))

      val tagSearch = new ListBuffer[QueryDefinition]
      tagSearch += matchQuery("tags.tags", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      language.foreach(lang => tagSearch += termQuery("tags.language", lang))

      val filterList = new ListBuffer[QueryDefinition]()
      license.foreach(license => filterList += nestedQuery("copyright.license").query(termQuery("copyright.license.license", license)))
      filterList += noCopyrightFilter

      val theSearch = search in ArticleApiProperties.SearchIndex -> ArticleApiProperties.SearchDocument query {
        bool {
          must(
            should(
              nestedQuery("titles").query {bool {must(titleSearch.toList)}},
              nestedQuery("article").query {bool {must(articleSearch.toList)}},
              nestedQuery("tags").query {bool {must(tagSearch.toList)}}
            ),
            filter (filterList)
          )
        }
      }
      executeSearch(theSearch, page, pageSize)
    }

    def executeSearch(search: SearchDefinition, page: Option[Int], pageSize: Option[Int]): SearchResult = {
      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      try {
        val response = elasticClient.execute {
          search start startAt limit numResults
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
