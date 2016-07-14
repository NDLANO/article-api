package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.model.{ImportStatus, RequiredLibrary}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.service.ExtractServiceComponent

trait H5PConverterModule {
  this: ExtractServiceComponent =>

  object H5PConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "h5p_content"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val nodeId = content.get("nid")

      logger.info(s"Converting h5p_content with nid $nodeId")
      val requiredLibraries = List(RequiredLibrary("text/javascript", "H5P-Resizer", "http://ndla.no/sites/all/modules/h5p/library/js/h5p-resizer.js"))
      val replacement = s"""<figure data-resource="h5p" data-id="${content.id}" data-url="http://ndla.no/h5p/embed/$nodeId"></figure>"""

      (replacement, requiredLibraries, ImportStatus(Seq(), visitedNodes))
    }
  }
}
