package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.ExtractServiceComponent

trait OppgaveConverterModule {
  this: ExtractServiceComponent =>

  object OppgaveConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "oppgave"

    override def convert(content: ContentBrowser): (String, Seq[RequiredLibrary], Seq[String]) = {
      val nodeId = content.get("nid")
      val messages = Seq[String]()
      val requiredLibraries = Seq[RequiredLibrary]()
      val oppgaves = extractService.getNodeOppgave(nodeId)

      oppgaves.find(x => x.language == content.language.getOrElse("")) match {
        case Some(oppgave) => (oppgave.content, requiredLibraries, messages)
        case None => {
          val errorMsg = s"Failed to retrieve 'oppgave' with language '$language' ($nodeId)"
          logger.warn(errorMsg)
          (s"{Import error: $errorMsg}", requiredLibraries, messages  :+ errorMsg)
        }
      }
    }
  }
}
