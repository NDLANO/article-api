package no.ndla.contentapi.batch.service.converters

import no.ndla.contentapi.batch.integration.ConverterModule
import org.jsoup.nodes.Element

import scala.collection.JavaConversions._

object SimpleTagConverter extends ConverterModule {

  def convert(el: Element): Element = {
    var elements = el.select("div")
    for (el <- elements) {
      el.className() match {
        case "full" | "paragraph" => el.unwrap()
      }
    }
    el
  }
}
