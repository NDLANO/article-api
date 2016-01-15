package no.ndla.contentapi.business

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.integration.AmazonIntegration


object SearchIndexer extends LazyLogging {

  val contentData = AmazonIntegration.getContentData()
  val contentIndex = AmazonIntegration.getContentIndex()

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
