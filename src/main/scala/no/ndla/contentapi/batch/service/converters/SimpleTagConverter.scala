package no.ndla.contentapi.batch.service.converters

import no.ndla.contentapi.batch.integration.ConverterModule
import org.jsoup.nodes.Element

import scala.collection.JavaConversions._

object SimpleTagConverter extends ConverterModule {

  def convert(el: Element): Element = {
    for (el <- el.select("div")) {
      el.className() match {
        case "full" | "paragraph" => el.unwrap()
        case "quote" => el.tagName("blockquote")
        case _ =>
      }
    }

    for (el <- el.select("pre")) {
      println("wrapping pre children")
      el.html("<code>" + el.html() + "</code")
    }
    el
  }
}
