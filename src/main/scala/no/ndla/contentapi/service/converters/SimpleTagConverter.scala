package no.ndla.contentapi.service.converters

import no.ndla.contentapi.integration.{ConverterModule, LanguageContent}
import no.ndla.contentapi.model.ImportStatus
import org.jsoup.nodes.Element

import scala.collection.JavaConversions._

object SimpleTagConverter extends ConverterModule {

  def convert(content: LanguageContent, importStatus: ImportStatus): (LanguageContent, ImportStatus) = {
    val element = stringToJsoupDocument(content.content)
    convertBody(element)
    convertDivs(element)
    convertPres(element)

    (content.copy(content=jsoupDocumentToString(element)), importStatus)
  }

  def convertDivs(el: Element) {
    for (el <- el.select("div")) {
      el.className() match {
        case "right" => replaceTag(el, "aside")
        case "paragraph" => replaceTag(el, "section")
        case "quote" => replaceTag(el, "blockquote")
        case "hide" => handle_hide(el)
        case "full" | "wrapicon" | "no_icon" => el.unwrap()
        case _ =>
      }
    }
  }

  def convertPres(el: Element) {
    for (el <- el.select("pre")) {
      el.html("<code>" + el.html() + "</code")
    }
  }

  def convertBody(el: Element) = el.select("body").tagName("article")

  private def replaceTag(el: Element, replacementTag: String) {
    el.tagName(replacementTag)
    el.removeAttr("class")
  }

  private def handle_hide(el: Element) {
    replaceTag(el, "details")
    el.select("a.re-collapse").remove()
    val details = el.select("div.details").html() // save content
    el.select("div.details").remove()
    val summary = el.text()
    el.html(s"<summary>$summary</summary>")
    el.append(details)
  }
}
