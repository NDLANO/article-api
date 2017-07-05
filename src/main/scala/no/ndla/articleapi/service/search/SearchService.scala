/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.search

import com.google.gson.JsonObject
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Count, Search, SearchResult => JestSearchResult}
import io.searchbox.params.Parameters
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.ElasticClient
import no.ndla.articleapi.model.api.{ArticleIntroduction, ArticleSummary, ArticleTitle, SearchResult, VisualElement}
import no.ndla.articleapi.model.domain._
import no.ndla.network.ApplicationUrl
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query._
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.{FieldSortBuilder, SortBuilders, SortOrder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait SearchService {
  this: ElasticClient with SearchIndexService with SearchConverterService =>
  val searchService: SearchService

  class SearchService extends LazyLogging {

    private val noCopyright = QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("license", "copyrighted"))

    def getHits(response: JestSearchResult, language: String): Seq[ArticleSummary] = {
      var resultList = Seq[ArticleSummary]()
      response.getTotal match {
        case count: Integer if count > 0 => {
          val resultArray = response.getJsonObject.get("hits").asInstanceOf[JsonObject].get("hits").getAsJsonArray
          val iterator = resultArray.iterator()
          while (iterator.hasNext) {
            resultList = resultList :+ hitAsArticleSummary(iterator.next().asInstanceOf[JsonObject].get("_source").asInstanceOf[JsonObject], language)
          }
          resultList
        }
        case _ => Seq()
      }
    }

    def hitAsArticleSummary(hit: JsonObject, language: String): ArticleSummary = {
      import scala.collection.JavaConversions._

      val titles = hit.get("title").getAsJsonObject.entrySet().to[Seq]
        .map(entr => ArticleTitle(entr.getValue.getAsString, Some(entr.getKey)))

      val supportedLanguages = titles.map(_.language.getOrElse(Language.NoLanguage))

      val title = titles
        .find(title => title.language.getOrElse(Language.NoLanguage) == (if (language == Language.AllLanguages) Language.DefaultLanguage else language))
        .getOrElse(titles.head)
        .title

      ArticleSummary(
        hit.get("id").getAsString,
        title,
        hit.get("visualElement").getAsJsonObject.entrySet().to[Seq].map(entr => VisualElement(entr.getValue.getAsString, Some(entr.getKey))),
        hit.get("introduction").getAsJsonObject.entrySet().to[Seq].map(entr => ArticleIntroduction(entr.getValue.getAsString, Some(entr.getKey))),
        ApplicationUrl.get + hit.get("id").getAsString,
        hit.get("license").getAsString,
        hit.get("articleType").getAsString,
        supportedLanguages
      )

    }

    def all(withIdIn: List[Long], language: String, license: Option[String], page: Int, pageSize: Int, sort: Sort.Value, articleTypes: Seq[String]): SearchResult = {
      logger.info(s"articletypes: $articleTypes")
      val fullSearch = QueryBuilders.boolQuery().filter(QueryBuilders.constantScoreQuery(QueryBuilders.termsQuery("articleType", articleTypes:_*)))
      executeSearch(withIdIn, language, license, sort, page, pageSize, fullSearch)
    }

    def matchingQuery(query: Iterable[String], withIdIn: List[Long], language: String, license: Option[String], page: Int, pageSize: Int, sort: Sort.Value, articleTypes: Seq[String]): SearchResult = {
      logger.info(s"articletypes: $articleTypes")
      val searchLanguage = if (language == Language.AllLanguages) Language.DefaultLanguage else language

      val titleSearch = QueryBuilders.matchQuery(s"title.$searchLanguage", query.mkString(" ")).operator(Operator.AND)
      val contentSearch = QueryBuilders.matchQuery(s"content.$searchLanguage", query.mkString(" ")).operator(Operator.AND)
      val tagSearch = QueryBuilders.matchQuery(s"tags.$searchLanguage", query.mkString(" ")).operator(Operator.AND)

      val fullSearch = QueryBuilders.boolQuery()
        .must(QueryBuilders.boolQuery()
          .should(QueryBuilders.nestedQuery("title", titleSearch, ScoreMode.Avg))
          .should(QueryBuilders.nestedQuery("content", contentSearch, ScoreMode.Avg))
          .should(QueryBuilders.nestedQuery("tags", tagSearch, ScoreMode.Avg)))
        .filter(QueryBuilders.constantScoreQuery(QueryBuilders.termsQuery("articleType", articleTypes:_*)))

      executeSearch(withIdIn, searchLanguage, license, sort, page, pageSize, fullSearch)
    }

    def executeSearch(withIdIn: List[Long], language: String, license: Option[String], sort: Sort.Value, page: Int, pageSize: Int, queryBuilder: BoolQueryBuilder): SearchResult = {
      val (filteredSearch, searchLanguage) = {
        val licenseFilteredSearch = license match {
          case None => queryBuilder.filter(noCopyright)
          case Some(lic) => queryBuilder.filter(QueryBuilders.termQuery("license", lic))
        }

        language match {
          case Language.AllLanguages => (licenseFilteredSearch, Language.DefaultLanguage)
          case _ => (licenseFilteredSearch.filter(QueryBuilders.nestedQuery("title", QueryBuilders.existsQuery(s"title.$language"), ScoreMode.Avg)), language)
        }
      }

      val idFilteredSearch = withIdIn match {
        case head :: tail => filteredSearch.filter(QueryBuilders.idsQuery(ArticleApiProperties.SearchDocument).addIds(head.toString :: tail.map(_.toString):_*))
        case Nil => filteredSearch
      }

      val searchQuery = new SearchSourceBuilder().query(idFilteredSearch).sort(getSortDefinition(sort, searchLanguage))

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val request = new Search.Builder(searchQuery.toString)
        .addIndex(ArticleApiProperties.SearchIndex)
        .setParameter(Parameters.SIZE, numResults) .setParameter("from", startAt)

        jestClient.execute(request.build()) match {
        case Success(response) => SearchResult(response.getTotal.toLong, page, numResults, language, getHits(response, language))
        case Failure(f) => errorHandler(Failure(f))
      }
    }

    def getSortDefinition(sort: Sort.Value, language: String): FieldSortBuilder = {
      sort match {
        case (Sort.ByTitleAsc) => SortBuilders.fieldSort(s"title.$language.raw").setNestedPath("title").order(SortOrder.ASC).missing("_last").unmappedType("string")
        case (Sort.ByTitleDesc) => SortBuilders.fieldSort(s"title.$language.raw").setNestedPath("title").order(SortOrder.DESC).missing("_last").unmappedType("string")
        case (Sort.ByRelevanceAsc) => SortBuilders.fieldSort("_score").order(SortOrder.ASC)
        case (Sort.ByRelevanceDesc) => SortBuilders.fieldSort("_score").order(SortOrder.DESC)
        case (Sort.ByLastUpdatedAsc) => SortBuilders.fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case (Sort.ByLastUpdatedDesc) => SortBuilders.fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case (Sort.ByIdAsc) => SortBuilders.fieldSort("id").order(SortOrder.ASC).missing("_last")
        case (Sort.ByIdDesc) => SortBuilders.fieldSort("id").order(SortOrder.DESC).missing("_last")
      }
    }

    def countDocuments(): Int = {
      val ret = jestClient.execute(
        new Count.Builder().addIndex(ArticleApiProperties.SearchIndex).build()
      ).map(result => result.getCount.toInt)
      ret.getOrElse(0)
    }

    def getStartAtAndNumResults(page: Int, pageSize: Int): (Int, Int) = {
      val numResults = pageSize.min(ArticleApiProperties.MaxPageSize)
      val startAt = (page - 1).max(0) * numResults

      (startAt, numResults)
    }

    private def errorHandler[T](failure: Failure[T]) = {
      failure match {
        case Failure(e: NdlaSearchException) => {
          e.getResponse.getResponseCode match {
            case notFound: Int if notFound == 404 => {
              logger.error(s"Index ${ArticleApiProperties.SearchIndex} not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              throw new IndexNotFoundException(s"Index ${ArticleApiProperties.SearchIndex} not found. Scheduling a reindex")
            }
            case _ => {
              logger.error(e.getResponse.getErrorMessage)
              throw new ElasticsearchException(s"Unable to execute search in ${ArticleApiProperties.SearchIndex}", e.getResponse.getErrorMessage)
            }
          }

        }
        case Failure(t: Throwable) => throw t
      }
    }

    private def scheduleIndexDocuments() = {
      val f = Future {
        searchIndexService.indexDocuments
      }

      f onFailure { case t => logger.warn("Unable to create index: " + t.getMessage, t) }
      f onSuccess {
        case Success(reindexResult)  => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }

}
