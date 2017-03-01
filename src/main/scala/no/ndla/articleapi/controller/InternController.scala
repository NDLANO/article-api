/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.controller

import no.ndla.articleapi.model.domain.ImportStatus
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.search.SearchIndexService
import no.ndla.articleapi.service.{ArticleContentInformation, ConverterService, ExtractConvertStoreContent, ExtractService}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{InternalServerError, Ok}

import scala.util.{Failure, Success}

trait InternController {
  this: ExtractService with ConverterService with ArticleRepository with ArticleContentInformation with ExtractConvertStoreContent with SearchIndexService =>
  val internController: InternController

  class InternController extends NdlaController {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    post("/index") {
      searchIndexService.indexDocuments match {
        case Success(reindexResult) => {
          val result = s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms."
          logger.info(result)
          Ok(result)
        }
        case Failure(f) => {
          logger.warn(f.getMessage, f)
          InternalServerError(f.getMessage)
        }
      }
    }

    post("/import/:external_id") {
      val externalId = params("external_id")

      extractConvertStoreContent.processNode(externalId) match {
        case Success((newId, status)) => ImportStatus(status.messages :+ s"Successfully imported node $externalId: $newId", status.visitedNodes)
        case Failure(exc) => errorHandler(exc)
      }
    }

    get("/tagsinuse") {
      ArticleContentInformation.getHtmlTagsMap
    }

    get("/embedurls/:external_subject_id") {
      ArticleContentInformation.getExternalEmbedResources(params("external_subject_id"))
    }

    get("/imagesinarticle/:article_id") {
      val id = long("article_id")
      ArticleContentInformation.getEmbedImageWithParentHtml(id)
    }
  }
}
