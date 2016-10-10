/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.model.{ImportStatus, RequiredLibrary}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.service.ExtractServiceComponent
import no.ndla.articleapi.service.converters.HtmlTagGenerator

trait H5PConverterModule {
  this: ExtractServiceComponent =>

  object H5PConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "h5p_content"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val nodeId = content.get("nid")

      logger.info(s"Converting h5p_content with nid $nodeId")
      val requiredLibraries = List(RequiredLibrary("text/javascript", "H5P-Resizer", "http://ndla.no/sites/all/modules/h5p/library/js/h5p-resizer.js"))
      val (replacement, figureUsageErrors) = HtmlTagGenerator.buildFigure(Map(
        "resource" -> "h5p",
        "id" -> s"${content.id}",
        "url" -> s"http://ndla.no/h5p/embed/$nodeId"))
      (replacement, requiredLibraries, ImportStatus(figureUsageErrors, visitedNodes))
    }
  }
}
