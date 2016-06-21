package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.model.RequiredLibrary
import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.service.ExtractServiceComponent

trait H5PConverterModule {
  this: ExtractServiceComponent =>

  object H5PConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "h5p_content"

    override def convert(content: ContentBrowser): (String, Seq[RequiredLibrary], Seq[String]) = {
      val requiredLibraries = List(RequiredLibrary("text/javascript", "H5P-Resizer", "http://ndla.no/sites/all/modules/h5p/library/js/h5p-resizer.js"))
      // TODO: iframe is only used here for demo purposes. Should be switched out with a proper alternative
      val replacement = s"""<iframe src="http://ndla.no/h5p/embed/${content.get("nid")}" ></iframe>"""

      (replacement, requiredLibraries, Seq[String]())
    }
  }
}
