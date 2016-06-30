package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.ExtractServiceComponent
import no.ndla.contentapi.ContentApiProperties.ndlaBaseHost

trait FagstoffConverterModule {
  this: ExtractServiceComponent =>

  object FagstoffConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "fagstoff"

    override def convert(content: ContentBrowser): (String, List[RequiredLibrary], List[String]) = {
      val nodeId = content.get("nid")
      val requiredLibraries = List[RequiredLibrary]()
      val fagstoffs = extractService.getNodeFagstoff(nodeId)

      fagstoffs.find(x => x.language == content.language.getOrElse("")) match {
        case Some(fagstoff) => {
          val (finalFagstoff, messages) = insertFagstoff(fagstoff.fagstoff, content)
          (finalFagstoff, requiredLibraries, messages)
        }
        case None => {
          val errorMsg = s"Failed to retrieve 'fagstoff' with language '${content.language.getOrElse("")}' ($nodeId)"
          logger.warn(errorMsg)
          (s"{Import error: $errorMsg}", requiredLibraries, List(errorMsg))
        }
      }
    }

    def insertFagstoff(fagstoff: String, contentBrowser: ContentBrowser): (String, List[String]) = {
      val insertionMethod = contentBrowser.get("insertion")
      insertionMethod match {
        case "inline" => (fagstoff, List[String]())
        case "collapsed_body" => (s"<details><summary>${contentBrowser.get("link_text")}</summary>$fagstoff</details>", List[String]())
        case "link" => {
          val warnMessage = s"""Link to old ndla.no ($ndlaBaseHost/node/${contentBrowser.get("nid")})"""
          logger.warn(warnMessage)
          (s"""<a href="$ndlaBaseHost/node/${contentBrowser.get("nid")}">${contentBrowser.get("link_text")}</a>""", List(warnMessage))
        }
        case _ => {
          val linkText = contentBrowser.get("link_text")
          val warnMessage = s"""Unhandled fagstoff insertion method '$insertionMethod' on '$linkText'. Defaulting to link."""
          logger.warn(warnMessage)
          (s"""<a href="$ndlaBaseHost/node/${contentBrowser.get("nid")}">$linkText</a>""", List(warnMessage))
        }
      }
    }
  }
}
