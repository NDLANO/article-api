package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.ContentApiProperties._
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.ExtractServiceComponent

trait GeneralContentConverterModule {
  this: ExtractServiceComponent =>

  abstract class GeneralContentConverter extends ContentBrowserConverterModule with LazyLogging {
    override def convert(contentBrowser: ContentBrowser): (String, Seq[RequiredLibrary], Seq[String]) = {
      val nodeId = contentBrowser.get("nid")
      val requiredLibraries = List[RequiredLibrary]()
      val contents = extractService.getNodeGeneralContent(nodeId)

      contents.find(x => x.language == contentBrowser.language.getOrElse("")) match {
        case Some(content) => {
          val (finalContent, messages) = insertContent(content.content, contentBrowser)
          (finalContent, requiredLibraries, messages)
        }
        case None => {
          val errorMsg = s"Failed to retrieve '$typeName' with language '${contentBrowser.language.getOrElse("")}' ($nodeId)"
          logger.warn(errorMsg)
          (s"{Import error: $errorMsg}", requiredLibraries, List(errorMsg))
        }
      }
    }

    def insertContent(content: String, contentBrowser: ContentBrowser): (String, List[String]) = {
      val insertionMethod = contentBrowser.get("insertion")
      insertionMethod match {
        case "inline" => (content, List[String]())
        case "collapsed_body" => (s"<details><summary>${contentBrowser.get("link_text")}</summary>$content</details>", List[String]())
        case "link" => {
          val warnMessage = s"""Link to old ndla.no ($ndlaBaseHost/node/${contentBrowser.get("nid")})"""
          logger.warn(warnMessage)
          (s"""<a href="$ndlaBaseHost/node/${contentBrowser.get("nid")}">${contentBrowser.get("link_text")}</a>""", List(warnMessage))
        }
        case _ => {
          val linkText = contentBrowser.get("link_text")
          val warnMessage = s"""Unhandled insertion method '$insertionMethod' on '$linkText'. Defaulting to link."""
          logger.warn(warnMessage)
          (s"""<a href="$ndlaBaseHost/node/${contentBrowser.get("nid")}">$linkText</a>""", List(warnMessage))
        }
      }
    }
  }
}
