package no.ndla.contentapi.controller

import com.typesafe.scalalogging.LazyLogging
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{Ok, ScalatraServlet}
import no.ndla.contentapi.business.SearchIndexer
import no.ndla.contentapi.model.Error
import no.ndla.contentapi.batch.BatchComponentRegistry.converterService

class AdminController extends ScalatraServlet with NativeJsonSupport with LazyLogging {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  post("/index") {
    Ok(SearchIndexer.indexDocuments())
  }

  post("/import/:node_id") {
    logger.info(s"Converting node ${params("node_id")}")
    Ok(converterService.convertNode(params("node_id")))
  }

  error{
    case t:Throwable => {
      logger.error(t.getMessage, t)
      halt(status = 500, body = Error(Error.GENERIC, t.getMessage))
    }
  }
}
