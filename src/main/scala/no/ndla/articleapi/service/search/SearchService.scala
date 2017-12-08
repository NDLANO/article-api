/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.search

import java.lang.Math.max

import com.google.gson.JsonObject
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Count, SearchResult}
import no.ndla.articleapi.ArticleApiProperties.{DefaultPageSize, MaxPageSize}
import no.ndla.articleapi.integration.ElasticClient
import no.ndla.articleapi.model.domain
import no.ndla.articleapi.model.domain._
import org.elasticsearch.script.Script
import org.elasticsearch.search.sort.ScriptSortBuilder.ScriptSortType
import org.elasticsearch.search.sort._

trait SearchService {
  this: ElasticClient with LazyLogging =>

  trait SearchService[T] {
    val searchIndex: String

    def hitToApiModel(hit: JsonObject, language: String): T

    def getHits(response: SearchResult, language: String): Seq[T] = {
      var resultList = Seq[T]()
      response.getTotal match {
        case count: Integer if count > 0 =>
          val resultArray = response.getJsonObject.get("hits").asInstanceOf[JsonObject].get("hits").getAsJsonArray
          val iterator = resultArray.iterator()
          while (iterator.hasNext) {
            resultList = resultList :+ hitToApiModel(iterator.next().asInstanceOf[JsonObject].get("_source").asInstanceOf[JsonObject], language)
          }
          resultList
        case _ => Seq.empty
      }
    }

    def getSortDefinition(sort: Sort.Value, language: String) = {
      val sortLanguage = language match {
        case domain.Language.NoLanguage => domain.Language.DefaultLanguage
        case _ => language
      }

      // Elasticsearch 'Painless' script for sorting by title if searching for all languages
      val supportedLanguages = Language.languageAnalyzers.map(la => la.lang).mkString("'", "', '", "'")
      val titleSortScript =
        s"""
           |int idx = -1;
           |String[] arr = new String[]{$supportedLanguages};
           |for (int i = arr.length-1; i >= 0; i--) {
           |  if(params['_source'].containsKey('title')){
           |    if(params['_source']['title'].containsKey(arr[i])){
           |      idx = i;
           |    }
           |  }
           |}
           |
           |if (idx != -1) {
           |  return params['_source']['title'][arr[idx]];
           |} else {
           |  return doc['id']; // Sort by id if there were no titles in supportedLanguages
           |}
           |""".stripMargin
      val script = new Script(titleSortScript)

      sort match {
        case (Sort.ByTitleAsc) =>
          language match {
            case "*" => SortBuilders.scriptSort(script, ScriptSortType.STRING).order(SortOrder.ASC)
            case _ => SortBuilders.fieldSort(s"title.$sortLanguage.raw").setNestedPath("title").order(SortOrder.ASC).missing("_last")
          }
        case (Sort.ByTitleDesc) =>
          language match {
            case "*" => SortBuilders.scriptSort(script, ScriptSortType.STRING).order(SortOrder.DESC)
            case _ => SortBuilders.fieldSort(s"title.$sortLanguage.raw").setNestedPath ("title").order (SortOrder.DESC).missing ("_last")
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
