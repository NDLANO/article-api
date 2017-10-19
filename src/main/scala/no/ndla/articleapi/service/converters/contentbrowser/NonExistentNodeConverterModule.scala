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

import scala.util.{Failure, Try}

trait NonExistentNodeConverterModule {

  object NonExistentNodeConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "NodeDoesNotExist"

    override def convert(content: ContentBrowser, importStatus: ImportStatus): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      Failure(ImportException(s"Found nonexistant node with id ${content.get("nid")}"))
    }
  }
}
