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
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.ExtractService
import no.ndla.articleapi.service.converters.HtmlTagGenerator

import scala.util.{Failure, Try}

trait UnsupportedContentConverter {
  this: ExtractService with HtmlTagGenerator =>

  object UnsupportedContentConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "unsupported content"

    override def convert(content: ContentBrowser, importStatus: ImportStatus): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      val nodeType = extractService.getNodeType(content.get("nid")).getOrElse("unknown")
      val errorMessage = s"Unsupported content $nodeType in node with id ${content.get("nid")}"
      logger.error(errorMessage)
      Failure(ImportException(errorMessage))
    }
  }

}
