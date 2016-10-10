/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.controller

import no.ndla.articleapi.model.ImportStatus
import no.ndla.articleapi.repository.ArticleRepositoryComponent
import no.ndla.articleapi.service.search.SearchIndexServiceComponent
import no.ndla.articleapi.service.{ConverterServiceComponent, ExtractConvertStoreContent, ExtractServiceComponent, ArticleContentInformation}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.Ok

import scala.util.{Failure, Success}

trait InternController {
  this: ExtractServiceComponent with ConverterServiceComponent with ArticleRepositoryComponent with ArticleContentInformation with ExtractConvertStoreContent with SearchIndexServiceComponent =>
  val internController: InternController

  class InternController extends NdlaController {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    post("/index") {
      Ok(searchIndexService.indexDocuments())
    }

    post("/import/:external_id") {
      val externalId = params("external_id")

      extractConvertStoreContent.processNode(externalId) match {
        case Success((newId, status)) => ImportStatus(status.messages :+ s"Successfully imported node $externalId: $newId", status.visitedNodes)
        case Failure(exc) => throw exc
      }
    }

    get("/tagsinuse") {
      ArticleContentInformation.getHtmlTagsMap
    }

    get("/embedurls/:external_subject_id") {
      ArticleContentInformation.getExternalEmbedResources(params("external_subject_id"))
    }
  }
}
