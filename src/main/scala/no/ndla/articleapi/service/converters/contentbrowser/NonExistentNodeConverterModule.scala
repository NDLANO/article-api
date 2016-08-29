/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.{ImportStatus, RequiredLibrary}

trait NonExistentNodeConverterModule {

  object NonExistentNodeConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "NodeDoesNotExist"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val message = s"Found nonexistant node with id ${content.get("nid")}"
      logger.warn(message)
      ("", List[RequiredLibrary](), ImportStatus(message, visitedNodes))
    }
  }
}
