package no.ndla.contentapi.batch.service.converters

import org.jsoup.nodes.Element
import no.ndla.contentapi.batch.integration.ConverterModule
import no.ndla.contentapi.batch.BatchComponentRegistry

import scala.collection.JavaConversions._

object ContentBrowserConverter extends ConverterModule {
  val cmData = BatchComponentRegistry.cmData

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

  def convertLink(nodeId: String, cont: ContentBrowser): String = {
    val (url, embedCode) = cmData.getNodeEmbedData(nodeId).get
    val youtubePattern = """https?://(?:www\.)?youtu(?:be\.com|\.be)(/.*)?""".r
    cont.get("insertion") match {
      case "inline" => {
        url match {
          case youtubePattern(_) => s"""<embed src="http://default/content" type="external/oembed" data-oembed="${url}" />"""
          case _ => embedCode
        }
      }
      case "link" => s"""<a href="${url}" title="${cont.get("link_title_text")}">${cont.get("link_text")}</a>"""
    }
  }

  def convert(el: Element): Element = {
    var isContentBrowserField = false

    do {
      val text = el.html()
      val cont = ContentBrowser(text)

      isContentBrowserField = cont.isContentBrowserField()
      if (isContentBrowserField) {
        val (start, end) = cont.getStartEndIndex()
        val nodeId = cont.get("nid")

        val newContent = cmData.getNodeType(cont.get("nid")) match {
          case Some("h5p_content") => s"""<embed src="http://default/content" type="external/oembed" data-oembed="http://ndla.no/node/${nodeId}" />"""
          case Some("lenke") => convertLink(nodeId, cont)
          case x => s"{CONTENT-${cont.get("nid")} " + x + "}"
        }

        el.html(text.substring(0, start) + newContent + text.substring(end))
      }
    } while (isContentBrowserField)
    el
  }
}
