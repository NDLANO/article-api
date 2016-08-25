package no.ndla.contentapi.controller

import no.ndla.contentapi.business.SearchIndexer
import no.ndla.contentapi.model.ImportStatus
import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service.{ConverterServiceComponent, ExtractConvertStoreContent, ExtractServiceComponent, HtmlTagsUsage}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.Ok

import scala.util.{Failure, Success}

trait InternController {
  this: ExtractServiceComponent with ConverterServiceComponent with ContentRepositoryComponent with HtmlTagsUsage with ExtractConvertStoreContent =>
  val internController: InternController

  class InternController extends NdlaController {

    protected implicit override val jsonFormats: Formats = DefaultFormats

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

    get("/tagsinuse") {
      HtmlTagsUsage.getHtmlTagsMap
    }

  }
}