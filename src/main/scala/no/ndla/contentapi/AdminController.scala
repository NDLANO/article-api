package no.ndla.contentapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.integration.AmazonIntegration
import no.ndla.contentapi.model.Error
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{Ok, ScalatraServlet}
import org.scalatra.json.NativeJsonSupport

class AdminController extends ScalatraServlet with NativeJsonSupport with LazyLogging {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  val contentData = AmazonIntegration.getContentData()
  val contentIndex = AmazonIntegration.getContentIndex()

  def indexDocuments() = {
    val start = System.currentTimeMillis

    val newIndexName = contentIndex.create()

    var numIndexed = 0
    contentData.applyToAll(docs => {
      numIndexed += contentIndex.indexDocuments(docs, newIndexName)
    })

    val oldIndexName = contentIndex.aliasTarget

    contentIndex.updateAliasTarget(oldIndexName, newIndexName)
    oldIndexName.foreach(contentIndex.delete)

    val result = s"Completed indexing of ${numIndexed} documents in ${System.currentTimeMillis() - start} ms."
    logger.info(result)
    result
  }

  post("/index") {
    Ok(indexDocuments())
  }

  error{
    case t:Throwable => {
      logger.error(t.getMessage, t)
      halt(status = 500, body = Error(Error.GENERIC, t.getMessage))
    }
  }

}
