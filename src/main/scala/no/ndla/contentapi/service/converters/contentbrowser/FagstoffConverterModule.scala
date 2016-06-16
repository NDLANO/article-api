package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.ExtractServiceComponent

trait FagstoffConverterModule {
  this: ExtractServiceComponent =>

  object FagstoffConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "fagstoff"

    override def convert(content: ContentBrowser): (String, List[RequiredLibrary], List[String]) = {
      val nodeId = content.get("nid")
      val messages = List[String]()
      val requiredLibraries = List[RequiredLibrary]()
      val fagstoffs = extractService.getNodeFagstoff(nodeId)

      fagstoffs.find(x => x.language == content.language.getOrElse("")) match {
        case Some(fagstoff) => (fagstoff.fagstoff, requiredLibraries, messages)
        case None => {
          val errorMsg = s"Failed to retrieve 'fagstoff' with language '${content.language.getOrElse("")}' ($nodeId)"
          logger.warn(errorMsg)
          (s"{Import error: $errorMsg}", requiredLibraries, messages :+ errorMsg)
        }
      }
    }
  }
}
