/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.repository.ArticleRepository

trait SearchIndexService {
  this: ArticleRepository with IndexService =>
  val searchIndexService: SearchIndexService

  class SearchIndexService extends LazyLogging {
    def indexDocuments() = {
      synchronized {
        val start = System.currentTimeMillis

        val newIndexName = indexService.createIndex()
        val oldIndexName = indexService.aliasTarget

        oldIndexName match {
          case None => indexService.updateAliasTarget(oldIndexName, newIndexName)
          case Some(_) =>
        }

        var numIndexed = 0
        articleRepository.applyToAll(docs => {
          numIndexed += indexService.indexDocuments(docs, newIndexName)
        })

        oldIndexName.foreach(indexName => {
          indexService.updateAliasTarget(oldIndexName, newIndexName)
          indexService.delete(indexName)
        })

        val result = s"Completed indexing of $numIndexed documents in ${System.currentTimeMillis() - start} ms."
        logger.info(result)
        result
      }
    }
  }

}
