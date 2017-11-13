/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.api.ImportException
import no.ndla.articleapi.model.domain.{Article, Concept, ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.ExtractConvertStoreContent
import no.ndla.articleapi.service.converters.HtmlTagGenerator
import no.ndla.validation.ValidationException

import scala.util.{Failure, Success, Try}

trait BegrepConverterModule {
  this: HtmlTagGenerator with ExtractConvertStoreContent =>

  object BegrepConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "begrep"

    override def convert(content: ContentBrowser, importStatus: ImportStatus): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      val nodeId = content.get("nid")
      extractConvertStoreContent.processNode(nodeId, importStatus) match {
        case Success((c: Concept, is)) =>
          val embedContent = HtmlTagGenerator.buildConceptEmbedContent(c.id.get, content.get("link_text"))
          Success((embedContent, Seq.empty, is.addMessage(s"Imported concept with id ${c.id}")))

        case Success((x: Article, _)) =>
          val msg = s"THIS IS A BUG: Imported begrep node with nid $nodeId but is marked as an article (id ${x.id})"
          logger.error(msg)
          Failure(ImportException(msg))
        case Failure(x) =>
          val exceptionMessage = x match {
            case ex: ValidationException => s"${ex.getMessage}: ${ex.errors.mkString(",")}"
            case ex => ex.getMessage
          }
          val msg = s"Failed to import begrep with node id $nodeId: $exceptionMessage"
          logger.error(msg)
          Failure(ImportException(msg))
      }

    }

  }
}
