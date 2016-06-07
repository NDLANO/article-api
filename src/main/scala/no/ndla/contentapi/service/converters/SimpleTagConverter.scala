package no.ndla.contentapi.service.converters

import no.ndla.contentapi.integration.ConverterModule
import no.ndla.contentapi.model.RequiredLibrary
import org.jsoup.Jsoup
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
    el.select("a.re-collapse").remove() // remove "hide" button
    val details = el.select("div.details").html() // save content
    el.select("div.details").remove() // remove content from element
    el.select("a.read-more").unwrap() // remove "show more" link
    val summary = el.text() // save show/hide text
    // replace old div-style element, with new details-summary element
    val newEl: Element = Jsoup.parseBodyFragment(s"<details><summary>$summary</summary>$details</details>").select("details")(0)
    el.replaceWith(newEl)
  }
}
