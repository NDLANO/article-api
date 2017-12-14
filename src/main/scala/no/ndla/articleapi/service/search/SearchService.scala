/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.search

import java.lang.Math.max

import com.google.gson.{JsonObject, JsonParser}
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Count, SearchResult}
import no.ndla.articleapi.ArticleApiProperties.{DefaultPageSize, MaxPageSize}
import no.ndla.articleapi.integration.ElasticClient
import no.ndla.articleapi.model.domain
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.service.ConverterService
import org.elasticsearch.script.Script
import org.elasticsearch.search.sort.ScriptSortBuilder.ScriptSortType
import org.elasticsearch.search.sort._
import org.json4s._
import org.json4s.native.JsonMethods._

trait SearchService {
  this: ElasticClient with ConverterService with LazyLogging =>

  trait SearchService[T] {
    val searchIndex: String

    def hitToApiModel(hit: JsonObject, language: String): T

    def getHits(response: SearchResult, language: String, hitToApi:(JsonObject, String) => T): Seq[T] = {
      response.getTotal match {
        case count: Integer if count > 0 =>
          val resultArray = (parse(response.getJsonString) \ "hits" \ "hits").asInstanceOf[JArray].arr

          resultArray.map(result => {
            val matchedLanguage = language match {
              case Language.AllLanguages | "*" => converterService.getLanguageFromHit(result).getOrElse(language)
              case _ => language
            }

            val hitString = compact(render(result \ "_source"))
            val jsonSource = new JsonParser().parse(hitString).getAsJsonObject
            hitToApi(jsonSource, matchedLanguage)
          })
        case _ => Seq()
      }
    }

    def getSortDefinition(sort: Sort.Value, language: String) = {
      val sortLanguage = language match {
        case domain.Language.NoLanguage => domain.Language.DefaultLanguage
        case _ => language
      }

      sort match {
        case (Sort.ByTitleAsc) =>
          language match {
            case "*" | Language.AllLanguages => SortBuilders.fieldSort("defaultTitle").order(SortOrder.ASC).missing("_last")
            case _ => SortBuilders.fieldSort(s"title.$sortLanguage.raw").setNestedPath("title").order(SortOrder.ASC).missing("_last")
          }
        case (Sort.ByTitleDesc) =>
          language match {
            case "*" | Language.AllLanguages => SortBuilders.fieldSort("defaultTitle").order(SortOrder.DESC).missing("_last")
            case _ => SortBuilders.fieldSort(s"title.$sortLanguage.raw").setNestedPath("title").order(SortOrder.DESC).missing("_last")
          }
        case (Sort.ByRelevanceAsc) => SortBuilders.fieldSort("_score").order(SortOrder.ASC)
        case (Sort.ByRelevanceDesc) => SortBuilders.fieldSort("_score").order(SortOrder.DESC)
        case (Sort.ByLastUpdatedAsc) => SortBuilders.fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case (Sort.ByLastUpdatedDesc) => SortBuilders.fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case (Sort.ByIdAsc) => SortBuilders.fieldSort("id").order(SortOrder.ASC).missing("_last")
        case (Sort.ByIdDesc) => SortBuilders.fieldSort("id").order(SortOrder.DESC).missing("_last")
      }
    }

    def countDocuments: Int = {
      val ret = jestClient.execute(
        new Count.Builder().addIndex(searchIndex).build()
      ).map(result => result.getCount.toInt)
      ret.getOrElse(0)
    }

    def getStartAtAndNumResults(page: Int, pageSize: Int): (Int, Int) = {
      val numResults = max(pageSize.min(MaxPageSize), 0)
      val startAt = (page - 1).max(0) * numResults

      (startAt, numResults)
    }

  }
}
