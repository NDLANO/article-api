/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ComponentRegistry

trait SearchIndexServiceComponent {
  val searchIndexService: SearchIndexService

  class SearchIndexService extends LazyLogging {

    val contentData = ComponentRegistry.articleRepository
    val contentIndex = ComponentRegistry.elasticContentIndex

    def indexDocuments() = {
      synchronized {
        val start = System.currentTimeMillis

        val newIndexName = contentIndex.create()
        val oldIndexName = contentIndex.aliasTarget

        oldIndexName match {
          case None => contentIndex.updateAliasTarget(oldIndexName, newIndexName)
          case Some(_) =>
        }

        var numIndexed = 0
        contentData.applyToAll(docs => {
          numIndexed += contentIndex.indexDocuments(docs, newIndexName)
        })

        oldIndexName.foreach(indexName => {
          contentIndex.updateAliasTarget(oldIndexName, newIndexName)
          contentIndex.delete(indexName)
        })

        val result = s"Completed indexing of $numIndexed documents in ${System.currentTimeMillis() - start} ms."
        logger.info(result)
        result
      }
    }
  }

}