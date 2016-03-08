package no.ndla.contentapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.business.SearchIndexer
import no.ndla.contentapi.model.Error
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{Ok, ScalatraServlet}

class AdminController extends ScalatraServlet with NativeJsonSupport with LazyLogging {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  post("/index") {
    Ok(SearchIndexer.indexDocuments())
  }

  error{
    case t:Throwable => {
      logger.error(t.getMessage, t)
      halt(status = 500, body = Error(Error.GENERIC, t.getMessage))
    }
  }
}
