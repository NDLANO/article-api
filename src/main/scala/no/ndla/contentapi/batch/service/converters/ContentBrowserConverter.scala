package no.ndla.contentapi.batch.service.converters

import org.jsoup.nodes.Element
import no.ndla.contentapi.batch.integration.ConverterModule
import no.ndla.contentapi.batch.service.ExtractServiceComponent

import scala.collection.JavaConversions._

trait ContentBrowserConverter {
  this: ExtractServiceComponent =>

  val contentBrowserConverter: ContentBrowserConverter

  class ContentBrowserConverter extends ConverterModule {
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

    def convert(el: Element): Element = {
      var isContentBrowserField = false

      do {
        val text = el.html()
        val cont = ContentBrowser(text)

        isContentBrowserField = cont.isContentBrowserField()
        if (isContentBrowserField) {
          val (start, end) = cont.getStartEndIndex()
          val nodeId = cont.get("nid")
          val newContent = extractService.getNodeType(cont.get("nid")) match {
            case Some("h5p_content") => s"""<embed data-oembed="http://ndla.no/node/${nodeId}" />"""
            case _ => s"{CONTENT-${cont.get("nid")}}"
          }

          el.html(text.substring(0, start) + newContent+ text.substring(end))
        }
      } while (isContentBrowserField)
      el
    }
  }
}
