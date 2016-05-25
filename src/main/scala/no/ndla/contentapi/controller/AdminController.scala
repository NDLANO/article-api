package no.ndla.contentapi.controller

import com.typesafe.scalalogging.LazyLogging
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{Ok, ScalatraServlet}
import no.ndla.contentapi.business.SearchIndexer
import no.ndla.contentapi.model.Error
import no.ndla.contentapi.batch.BatchComponentRegistry.converterService
import no.ndla.contentapi.integration.AmazonIntegration

class AdminController extends ScalatraServlet with NativeJsonSupport with LazyLogging {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  post("/index") {
    Ok(SearchIndexer.indexDocuments())
  }

  post("/import/:node_id") {
    logger.info("Converting node {}", params("node_id"))
    val contentData = AmazonIntegration.getContentData()
    val nodeId = params("node_id")
    val node = converterService.convertNode(nodeId)

    contentData.exists(nodeId) match {
      case true => contentData.update(node, nodeId)
      case false => contentData.insert(node, nodeId)
    }

    Ok()
  }

  error{
    case t:Throwable => {
      logger.error(t.getMessage, t)
      halt(status = 500, body = Error(Error.GENERIC, t.getMessage))
    }
  }
}
