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
        case "full" => el.unwrap()
        case "paragraph" => {
          el.tagName("section")
          el.removeAttr("class")
        }
        case "quote" => {
          el.tagName("blockquote")
          el.removeAttr("class")
        }
        case _ =>
      }
    }

    for (el <- el.select("pre")) {
      el.html("<code>" + el.html() + "</code")
    }
    (el, List[RequiredLibrary](), List[String]())
  }
}
