/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.ExtractService
import no.ndla.articleapi.service.converters.HtmlTagGenerator

trait UnsupportedContentConverter {
  this: ExtractService with HtmlTagGenerator =>

  object UnsupportedContentConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "unsupported content"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val nodeType = extractService.getNodeType(content.get("nid")).getOrElse("unknown")
      val errorMessage = s"""Unsupported content ($nodeType): ${content.get("nid")}"""

      logger.warn(errorMessage)
      val convertedContent = HtmlTagGenerator.buildErrorContent(errorMessage)
      (convertedContent, Seq(), ImportStatus(errorMessage, visitedNodes))
    }
  }

}
