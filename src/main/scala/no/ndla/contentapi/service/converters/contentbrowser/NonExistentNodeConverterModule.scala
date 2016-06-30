package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.RequiredLibrary

trait NonExistentNodeConverterModule {

  object NonExistentNodeConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "NodeDoesNotExist"

    override def convert(content: ContentBrowser): (String, Seq[RequiredLibrary], Seq[String]) = {
      val message = s"Found nonexistant node with id ${content.get("nid")}"
      logger.warn(message)
      ("", List[RequiredLibrary](), List(message))
    }
  }
}
