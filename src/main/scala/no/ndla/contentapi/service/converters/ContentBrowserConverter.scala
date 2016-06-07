package no.ndla.contentapi.service.converters

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.integration.ConverterModule
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.{ExtractServiceComponent, ImageApiServiceComponent}
import org.jsoup.nodes.Element
import com.netaporter.uri.dsl._

trait ContentBrowserConverter {
  this: ExtractServiceComponent with ImageApiServiceComponent =>

  val contentBrowserConverter: ContentBrowserConverter

  class ContentBrowserConverter extends ConverterModule with LazyLogging {
    case class ContentBrowser(contentBrowserString: String) {
      // Extract the contentbrowser variables
      private val Pattern = """(?s).*\[contentbrowser (.*) contentbrowser\].*""".r
      private val ContentField = contentBrowserString match {
        case Pattern(group) => group
        case _ => ""
      }

      // Extract every key-value pair and build a map
      private val KeyVal = ContentField.split("==").map(x => x.stripPrefix("=").split("="))
      private val FieldMap = KeyVal.map(el => el(0) -> (if (el.length > 1) el(1) else "")).toMap

      def isContentBrowserField(): Boolean = {
        contentBrowserString.matches(Pattern.toString)
      }

      def getStartEndIndex(): (Int, Int) = {
        val (startIdf, endIdf) = ("[contentbrowser ", " contentbrowser]")
        val a = contentBrowserString.indexOf(ContentField)
        (a - startIdf.length(), a + ContentField.length() + endIdf.length())
      }

      def get(key: String): String = {
        FieldMap.get(key).get
      }
    }

    def convertLink(nodeId: String, cont: ContentBrowser): (String, List[String]) = {
      var errors = List[String]()
      val (url, embedCode) = extractService.getNodeEmbedData(nodeId).get
      val youtubePattern = """https?://(?:www\.)?youtu(?:be\.com|\.be)(/.*)?""".r
      val NDLAPattern = """.*(ndla.no).*""".r

      url.host match {
        case NDLAPattern(_) => {
          errors = errors :+ s"(Warning) Link to NDLA resource '${url}'"
          logger.warn("Link to NDLA resource: '{}'", url)
        }
        case _ =>
      }

      val converted = cont.get("insertion") match {
        case "inline" => {
          url match {
            case youtubePattern(_) => s"""<embed src="http://default/content" type="external/oembed" data-oembed="${url}" />"""
            case _ => embedCode
          }
        }
        case "link" => s"""<a href="${url}" title="${cont.get("link_title_text")}">${cont.get("link_text")}</a>"""
      }
      (converted, errors)
    }

    def getImage(cont: ContentBrowser): (String, List[String]) = {
      var errors = List[String]()

      val imageTag = imageApiService.getMetaByExternId(cont.get("nid")) match {
        case Some(image) => s"""<img src="/images/${image.images.full.get.url}" alt="${cont.get("alt")}" />"""
        case None => {
          errors = errors :+ s"Image with id ${cont.get("nid")} was not found"
          s"<img src='stock.jpeg' alt='The image with id ${cont.get("nid")} was not not found' />"
        }
      }
      (imageTag, errors)
    }

    def convert(el: Element): (Element, List[RequiredLibrary], List[String]) = {
      var isContentBrowserField = false
      var requiredLibraries = List[RequiredLibrary]()
      var errors = List[String]()

      do {
        val text = el.html()
        val cont = ContentBrowser(text)

        isContentBrowserField = cont.isContentBrowserField()
        if (isContentBrowserField) {
          val (start, end) = cont.getStartEndIndex()
          val nodeId = cont.get("nid")
          val newContent = extractService.getNodeType(cont.get("nid")) match {
            case Some("h5p_content") => {
              requiredLibraries find {el => el.name == "H5P-Resizer"} match {
                case None => requiredLibraries = requiredLibraries :+ RequiredLibrary("text/javascript", "H5P-Resizer", "http://ndla.no/sites/all/modules/h5p/library/js/h5p-resizer.js")
                case _ =>
              }
              s"""<embed data-oembed="http://ndla.no/h5p/embed/${nodeId}" />"""
            }
            case Some("image") => {
              val (replacement, errorMsgs) = getImage(cont)
              errors = errors ::: errorMsgs
              replacement
            }
            case Some("lenke") => {
              val (content, errorMsgs) = convertLink(nodeId, cont)
              errors = errors ::: errorMsgs
              content
            }
            case _ => {
              errors = errors :+ s"Unsupported content: ${nodeId}"
              logger.warn("ContentBrowserConverter: Unsupported content ({})", nodeId)
              s"{Unsupported content: ${nodeId}}"
            }
          }
          el.html(text.substring(0, start) + newContent+ text.substring(end))
        }
      } while (isContentBrowserField)
      (el, requiredLibraries, errors)
    }
  }
}
