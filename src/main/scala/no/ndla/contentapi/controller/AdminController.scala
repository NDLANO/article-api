package no.ndla.contentapi.controller

import com.typesafe.scalalogging.LazyLogging
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{Ok, ScalatraServlet}
import no.ndla.contentapi.business.SearchIndexer
import no.ndla.contentapi.model.Error
import no.ndla.contentapi.batch.BatchComponentRegistry.{contentData, converterService, extractorService}

class AdminController extends ScalatraServlet with NativeJsonSupport with LazyLogging {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  post("/index") {
    Ok(SearchIndexer.indexDocuments())
  }

  post("/import/:node_id") {
    val nodeId = params("node_id")
    val node = extractorService.importNode(nodeId)
    val convertedNode = converterService.convertNode(node)

    logger.info("Converting node {}", nodeId)

    contentData.exists(nodeId) match {
      case true => contentData.update(convertedNode, nodeId)
      case false => contentData.insert(convertedNode, nodeId)
    }

    Ok("Imported Node " + nodeId)
  }

  error{
    case t:Throwable => {
      logger.error(t.getMessage, t)
      halt(status = 500, body = Error(Error.GENERIC, t.getMessage))
    }
  }
}
