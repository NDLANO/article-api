/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import java.lang.Math.max
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties.{DefaultLanguage, ElasticSearchScrollKeepAlive, MaxPageSize}
import no.ndla.articleapi.integration.Elastic4sClient
import no.ndla.articleapi.model.domain
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.search.SearchResult
import no.ndla.articleapi.service.ConverterService
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException

import scala.util.{Failure, Success, Try}

trait SearchService {
  this: Elastic4sClient with ConverterService with LazyLogging =>

  trait SearchService[T] {
    val searchIndex: String

    def scroll(scrollId: String, language: String, fallback: Boolean): Try[SearchResult[T]] =
      e4sClient
        .execute {
          searchScroll(scrollId, ElasticSearchScrollKeepAlive)
        }
        .map(response => {
          val hits = getHits(response.result, language, fallback)
          SearchResult[T](
            totalCount = response.result.totalHits,
            page = None,
            pageSize = response.result.hits.hits.length,
            language = language,
            results = hits,
            scrollId = response.result.scrollId
          )
        })

    /**
      * Returns hit as summary
      *
      * @param hit as json string
      * @param language language as ISO639 code
      * @return api-model summary of hit
      */
    def hitToApiModel(hit: String, language: String): T

    def getHits(response: SearchResponse, language: String, fallback: Boolean): Seq[T] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits.toList

          resultArray.map(result => {
            val matchedLanguage = language match {
              case Language.AllLanguages =>
                converterService.getLanguageFromHit(result).getOrElse(language)
              case _ => language
            }

            hitToApiModel(result.sourceAsString, matchedLanguage)
          })
        case _ => Seq()
      }
    }

    def getSortDefinition(sort: Sort.Value, language: String): FieldSort = {
      val sortLanguage = language match {
        case domain.Language.NoLanguage => DefaultLanguage
        case _                          => language
      }

      sort match {
        case Sort.ByTitleAsc =>
          language match {
            case Language.AllLanguages => fieldSort("defaultTitle").order(SortOrder.Asc).missing("_last")
            case _                     => fieldSort(s"title.$sortLanguage.raw").order(SortOrder.Asc).missing("_last").unmappedType("long")
          }
        case Sort.ByTitleDesc =>
          language match {
            case Language.AllLanguages => fieldSort("defaultTitle").order(SortOrder.Desc).missing("_last")
            case _                     => fieldSort(s"title.$sortLanguage.raw").order(SortOrder.Desc).missing("_last").unmappedType("long")
          }
        case Sort.ByRelevanceAsc    => fieldSort("_score").order(SortOrder.Asc)
        case Sort.ByRelevanceDesc   => fieldSort("_score").order(SortOrder.Desc)
        case Sort.ByLastUpdatedAsc  => fieldSort("lastUpdated").order(SortOrder.Asc).missing("_last")
        case Sort.ByLastUpdatedDesc => fieldSort("lastUpdated").order(SortOrder.Desc).missing("_last")
        case Sort.ByIdAsc           => fieldSort("id").order(SortOrder.Asc).missing("_last")
        case Sort.ByIdDesc          => fieldSort("id").order(SortOrder.Desc).missing("_last")
      }
    }

    def countDocuments: Long = {
      val response = e4sClient.execute {
        catCount(searchIndex)
      }

      response match {
        case Success(resp) => resp.result.count
        case Failure(_)    => 0
      }
    }

    def getStartAtAndNumResults(page: Int, pageSize: Int): (Int, Int) = {
      val numResults = max(pageSize.min(MaxPageSize), 0)
      val startAt = (page - 1).max(0) * numResults

      (startAt, numResults)
    }

    protected def scheduleIndexDocuments(): Unit

    protected def errorHandler[U](failure: Throwable): Failure[U] = {
      failure match {
        case e: NdlaSearchException =>
          e.rf.status match {
            case notFound: Int if notFound == 404 =>
              logger.error(s"Index $searchIndex not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              Failure(new IndexNotFoundException(s"Index $searchIndex not found. Scheduling a reindex"))
            case _ =>
              logger.error(e.getMessage)
              Failure(new ElasticsearchException(s"Unable to execute search in $searchIndex", e.getMessage))
          }
        case t: Throwable => Failure(t)
      }
    }

  }
}
