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
import com.sksamuel.elastic4s.searches.sort.{FieldSortDefinition, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties.MaxPageSize
import no.ndla.articleapi.integration.Elastic4sClient
import no.ndla.articleapi.model.domain
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.service.ConverterService
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.native.JsonMethods._

import scala.util.{Failure, Success}

trait SearchService {
  this: Elastic4sClient with ConverterService with LazyLogging =>

  trait SearchService[T] {
    val searchIndex: String

    /**
      * Returns hit as summary
      * @param hit as json string
      * @param language language as ISO639 code
      * @return api-model summary of hit
      */
    def hitToApiModel(hit: String, language: String): T

    def getHits(response: SearchResponse, language: String, fallback: Boolean): Seq[T] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits

          resultArray.map(result => {
            val matchedLanguage = language match {
              case Language.AllLanguages | "*" =>
                converterService.getLanguageFromHit(result).getOrElse(language)
              case _ => language
            }

            hitToApiModel(result.sourceAsString, matchedLanguage)
          })
        case _ => Seq()
      }
    }

    def getSortDefinition(sort: Sort.Value, language: String): FieldSortDefinition = {
      val sortLanguage = language match {
        case domain.Language.NoLanguage => domain.Language.DefaultLanguage
        case _                          => language
      }

      sort match {
        case (Sort.ByTitleAsc) =>
          language match {
            case "*" | Language.AllLanguages => fieldSort("defaultTitle").order(SortOrder.ASC).missing("_last")
            case _                           => fieldSort(s"title.$sortLanguage.raw").order(SortOrder.ASC).missing("_last")
          }
        case (Sort.ByTitleDesc) =>
          language match {
            case "*" | Language.AllLanguages => fieldSort("defaultTitle").order(SortOrder.DESC).missing("_last")
            case _                           => fieldSort(s"title.$sortLanguage.raw").order(SortOrder.DESC).missing("_last")
          }
        case (Sort.ByRelevanceAsc)    => fieldSort("_score").order(SortOrder.ASC)
        case (Sort.ByRelevanceDesc)   => fieldSort("_score").order(SortOrder.DESC)
        case (Sort.ByLastUpdatedAsc)  => fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case (Sort.ByLastUpdatedDesc) => fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case (Sort.ByIdAsc)           => fieldSort("id").order(SortOrder.ASC).missing("_last")
        case (Sort.ByIdDesc)          => fieldSort("id").order(SortOrder.DESC).missing("_last")
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
