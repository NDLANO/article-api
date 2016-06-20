package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.ExtractServiceComponent


trait AktualitetConverterModule {
  this: ExtractServiceComponent =>

  object AktualitetConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "aktualitet"

    override def convert(content: ContentBrowser): (String, List[RequiredLibrary], List[String]) = {
      val nodeId = content.get("nid")
      val messages = List[String]()
      val requiredLibraries = List[RequiredLibrary]()
      val aktualitet = extractService.getNodeAktualitet(nodeId)

      aktualitet match {
        case Some(aktualitet) => (aktualitet.aktualitet, requiredLibraries, messages)
        case None => {
          val errorMsg = s"Failed to retrieve 'aktualitet' ($nodeId)"
          logger.warn(errorMsg)
          (s"{Import error: $errorMsg}", requiredLibraries, messages  :+ errorMsg)
        }
      }
    }
  }
}
