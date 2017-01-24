/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.model.domain.{Article, ReindexResult}
import no.ndla.articleapi.repository.ArticleRepository

import scala.util.{Failure, Success, Try}

trait SearchIndexService {
  this: ArticleRepository with IndexService =>
  val searchIndexService: SearchIndexService

  class SearchIndexService extends LazyLogging {

    def indexDocument(imported: Article): Try[Any] = {
      for {
        _ <- indexService.aliasTarget.map {
          case Some(index) => Success(index)
          case None => indexService.createIndex().map(newIndex => indexService.updateAliasTarget(None, newIndex))
        }
        imported <- indexService.indexDocument(imported)
      } yield imported
    }

    def indexDocuments: Try[ReindexResult] = {
      synchronized {
        val start = System.currentTimeMillis()
        indexService.createIndex().flatMap(indexName => {
          val operations = for {
            numIndexed <- sendToElastic(indexName)
            aliasTarget <- indexService.aliasTarget
            updatedTarget <- indexService.updateAliasTarget(aliasTarget, indexName)
            deleted <- indexService.delete(aliasTarget)
          } yield numIndexed

          operations match {
            case Failure(f) => {
              indexService.delete(Some(indexName))
              Failure(f)
            }
            case Success(totalIndexed) => {
              Success(ReindexResult(totalIndexed, System.currentTimeMillis() - start))
            }
          }
        })
      }
    }

    def sendToElastic(indexName: String): Try[Int] = {
      var numIndexed = 0
      getRanges.map(ranges => {
        ranges.foreach(range => {
          val numberInBulk = indexService.indexDocuments(articleRepository.articlesWithIdBetween(range._1, range._2), indexName)
          numberInBulk match {
            case Success(num) => numIndexed += num
            case Failure(f) => return Failure(f)
          }
        })
        numIndexed
      })
    }

    def getRanges:Try[List[(Long,Long)]] = {
      Try{
        val (minId, maxId) = articleRepository.minMaxId
        Seq.range(minId, maxId).grouped(ArticleApiProperties.IndexBulkSize).map(group => (group.head, group.last + 1)).toList
      }
    }
  }
}
