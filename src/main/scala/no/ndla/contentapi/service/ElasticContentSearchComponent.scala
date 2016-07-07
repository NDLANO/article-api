package no.ndla.contentapi.service

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.ContentApiProperties
import no.ndla.contentapi.business.{ContentSearch, SearchIndexer}
import no.ndla.contentapi.integration.ElasticClientComponent
import no.ndla.contentapi.model.ContentSummary
import no.ndla.network.ApplicationUrl
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.transport.RemoteTransportException

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ElasticContentSearchComponent {
  this: ElasticClientComponent =>
  val elasticContentSearch: ElasticContentSearch

  class ElasticContentSearch extends ContentSearch with LazyLogging {

    val noCopyrightFilter = not(nestedQuery("copyright.license").query(termQuery("copyright.license.license", "copyrighted")))

    implicit object ContentHitAs extends HitAs[ContentSummary] {
      override def as(hit: RichSearchHit): ContentSummary = {
        val sourceMap = hit.sourceAsMap
        ContentSummary(
          sourceMap("id").toString,
          sourceMap("titles").asInstanceOf[java.util.ArrayList[AnyRef]].get(0).asInstanceOf[java.util.HashMap[String, String]].get("title"),
          ApplicationUrl.get + sourceMap("id").toString,
          sourceMap("copyright").asInstanceOf[java.util.HashMap[String, AnyRef]].get("license").asInstanceOf[java.util.HashMap[String, String]].get("license"))
      }
    }

    override def all(license: Option[String], page: Option[Int], pageSize: Option[Int]): Iterable[ContentSummary] = {
      val filterList = new ListBuffer[QueryDefinition]()
      license.foreach(license => filterList += nestedQuery("copyright.license").query(termQuery("copyright.license.license", license)))
      filterList += noCopyrightFilter

      val theSearch = search in ContentApiProperties.SearchIndex -> ContentApiProperties.SearchDocument query filter(filterList)
      theSearch.sort(field sort "id")

      executeSearch(theSearch, page, pageSize)
    }

    override def matchingQuery(query: Iterable[String], language: Option[String], license: Option[String], page: Option[Int], pageSize: Option[Int]): Iterable[ContentSummary] = {
      val titleSearch = new ListBuffer[QueryDefinition]
      titleSearch += matchQuery("titles.title", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      language.foreach(lang => titleSearch += termQuery("titles.language", lang))

      val contentSearch = new ListBuffer[QueryDefinition]
      contentSearch += matchQuery("content.content", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      language.foreach(lang => contentSearch += termQuery("content.language", lang))

      val tagSearch = new ListBuffer[QueryDefinition]
      tagSearch += matchQuery("tags.tags", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      language.foreach(lang => tagSearch += termQuery("tags.language", lang))

      val filterList = new ListBuffer[QueryDefinition]()
      license.foreach(license => filterList += nestedQuery("copyright.license").query(termQuery("copyright.license.license", license)))
      filterList += noCopyrightFilter

      val theSearch = search in ContentApiProperties.SearchIndex -> ContentApiProperties.SearchDocument query {
        bool {
          must(
            should(
              nestedQuery("titles").query {bool {must(titleSearch.toList)}},
              nestedQuery("content").query {bool {must(contentSearch.toList)}},
              nestedQuery("tags").query {bool {must(tagSearch.toList)}}
            ),
            filter (filterList)
          )
        }
      }
      executeSearch(theSearch, page, pageSize)
    }

    def executeSearch(search: SearchDefinition, page: Option[Int], pageSize: Option[Int]) = {
      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      try {
        elasticClient.execute {
          search start startAt limit numResults
        }.await.as[ContentSummary]
      } catch {
        case e: RemoteTransportException => errorHandler(e.getCause)
      }
    }

    def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
      val numResults = pageSize match {
        case Some(num) =>
          if (num > 0) num.min(ContentApiProperties.MaxPageSize) else ContentApiProperties.DefaultPageSize
        case None => ContentApiProperties.DefaultPageSize
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
        SearchIndexer.indexDocuments()
      }
      f onFailure { case t => logger.error("Unable to create index: " + t.getMessage) }
    }
  }

}
