package no.ndla.contentapi.service.converters

import no.ndla.contentapi.integration.ConverterModule
import no.ndla.contentapi.model.RequiredLibrary
import org.jsoup.nodes.Element

import scala.collection.JavaConversions._

object SimpleTagConverter extends ConverterModule {

  def convert(el: Element): (Element, List[RequiredLibrary], List[String]) = {
    var elements = el.select("div")
    for (el <- elements) {
      el.className() match {
        case "right" => replaceTag(el, "aside")
        case "paragraph" => replaceTag(el, "section")
        case "quote" => replaceTag(el, "blockquote")
        case "hide" => handle_hide(el)
        case "full" | "wrapicon" | "no_icon" => el.unwrap()
        case _ =>
      }
    }

    for (el <- el.select("pre")) {
      el.html("<code>" + el.html() + "</code")
    }
    (el, List[RequiredLibrary](), List[String]())
  }

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
