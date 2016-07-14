package no.ndla.contentapi.controller

import com.typesafe.scalalogging.LazyLogging
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{Ok, ScalatraServlet}
import no.ndla.contentapi.business.SearchIndexer
import no.ndla.contentapi.model.{Error, ImportStatus, NodeNotFoundException}
import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service.{ConverterServiceComponent, ExtractConvertStoreContent, ExtractServiceComponent}
import no.ndla.logging.LoggerContext
import no.ndla.network.ApplicationUrl

import scala.util.{Failure, Success}

trait InternController {
  this: ExtractConvertStoreContent =>
  val internController: InternController

  class InternController extends ScalatraServlet with NativeJsonSupport with LazyLogging {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    before() {
      contentType = formats("json")
      LoggerContext.setCorrelationID(Option(request.getHeader("X-Correlation-ID")))
      ApplicationUrl.set(request)
    }

    after() {
      LoggerContext.clearCorrelationID()
      ApplicationUrl.clear()
    }

    error {
      case t: Throwable => {
        logger.error(t.getMessage, t)
        halt(status = 500, body = Error(Error.GENERIC, t.getMessage))
      }
    }

    post("/index") {
      Ok(SearchIndexer.indexDocuments())
    }

    post("/import/:external_id") {
      val externalId = params("external_id")

      extractConvertStoreContent.processNode(externalId) match {
        case Success((newId, status)) => ImportStatus(status.messages :+ s"Successfully imported node $externalId: $newId", status.visitedNodes)
        case Failure(exc) => throw exc
      }
    }

  }
}