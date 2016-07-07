package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.ExtractServiceComponent
import no.ndla.contentapi.ContentApiProperties.ndlaBaseHost

trait AktualitetConverterModule {
  this: ExtractServiceComponent =>

  object AktualitetConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "aktualitet"

    override def convert(content: ContentBrowser): (String, List[RequiredLibrary], List[String]) = {
      val nodeId = content.get("nid")
      val requiredLibraries = List[RequiredLibrary]()
      val aktualitet = extractService.getNodeAktualitet(nodeId)

      logger.info(s"Converting aktualitet with nid $nodeId")

      aktualitet.find(x => x.language == content.language.getOrElse("")) match {
        case Some(aktualitet) => {
          val (finalAktualitet, messages) = insertAktualitet(aktualitet.aktualitet, content)
          (finalAktualitet, requiredLibraries, messages)
        }
        case None => {
          val errorMsg = s"Failed to retrieve 'aktualitet' ($nodeId)"
          logger.warn(errorMsg)
          (s"{Import error: $errorMsg}", requiredLibraries, List(errorMsg))
        }
      }
    }

    def insertAktualitet(aktualitet: String, contentBrowser: ContentBrowser): (String, List[String]) = {
      val insertionMethod = contentBrowser.get("insertion")
      insertionMethod match {
        case "inline" => (aktualitet, List[String]())
        case "collapsed_body" => (s"<details><summary>${contentBrowser.get("link_text")}</summary>$aktualitet</details>", List[String]())
        case "link" => {
          val warnMessage = s"""Link to old ndla.no ($ndlaBaseHost/node/${contentBrowser.get("nid")})"""
          logger.warn(warnMessage)
          (s"""<a href="$ndlaBaseHost/node/${contentBrowser.get("nid")}">${contentBrowser.get("link_text")}</a>""", List(warnMessage))
        }
        case _ => {
          val linkText = contentBrowser.get("link_text")
          val warnMessage = s"""Unhandled aktualitet insertion method '$insertionMethod' on '$linkText'. Defaulting to link."""
          logger.warn(warnMessage)
          (s"""<a href="$ndlaBaseHost/node/${contentBrowser.get("nid")}">$linkText</a>""", List(warnMessage))
        }
      }
    }
  }
}
