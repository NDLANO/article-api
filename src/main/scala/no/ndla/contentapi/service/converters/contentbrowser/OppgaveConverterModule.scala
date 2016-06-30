package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.ExtractServiceComponent
import no.ndla.contentapi.ContentApiProperties.ndlaBaseHost

trait OppgaveConverterModule {
  this: ExtractServiceComponent =>

  object OppgaveConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "oppgave"

    override def convert(content: ContentBrowser): (String, Seq[RequiredLibrary], Seq[String]) = {
      val nodeId = content.get("nid")
      val requiredLibraries = Seq[RequiredLibrary]()
      val oppgaves = extractService.getNodeOppgave(nodeId)

      oppgaves.find(x => x.language == content.language.getOrElse("")) match {
        case Some(oppgave) => {
          val (finalOppgave, messages) = insertOppgave(oppgave.content, content)
          (finalOppgave, requiredLibraries, messages)
        }
        case None => {
          val errorMsg = s"Failed to retrieve 'oppgave' with language '${content.language.getOrElse("")}' ($nodeId)"
          logger.warn(errorMsg)
          (s"{Import error: $errorMsg}", requiredLibraries, List(errorMsg))
        }
      }
    }

    def insertOppgave(oppgave: String, contentBrowser: ContentBrowser): (String, List[String]) = {
      val insertionMethod = contentBrowser.get("insertion")
      insertionMethod match {
        case "inline" => (oppgave, List[String]())
        case "collapsed_body" => (s"<details><summary>${contentBrowser.get("link_text")}</summary>$oppgave</details>", List[String]())
        case "link" | "lightbox_large" => {
          val warnMessage = s"""Link to old ndla.no ($ndlaBaseHost/node/${contentBrowser.get("nid")})"""
          logger.warn(warnMessage)
          (s"""<a href="$ndlaBaseHost/node/${contentBrowser.get("nid")}">${contentBrowser.get("link_text")}</a>""", List(warnMessage))
        }
        case _ => {
          val linkText = contentBrowser.get("link_text")
          val warnMessage = s"""Unhandled oppgave insertion method '$insertionMethod' on '$linkText'. Defaulting to link."""
          logger.warn(warnMessage)
          (s"""<a href="$ndlaBaseHost/node/${contentBrowser.get("nid")}">$linkText</a>""", List(warnMessage))
        }
      }
    }
  }
}
