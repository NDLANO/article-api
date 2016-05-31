package no.ndla.contentapi.service.converters

import no.ndla.contentapi.integration.ConverterModule
import no.ndla.contentapi.model.RequiredLibrary
import org.jsoup.nodes.Element

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object SimpleTagConverter extends ConverterModule {

  def convert(el: Element)(implicit requiredLibraries: ListBuffer[RequiredLibrary]): Element = {
    var elements = el.select("div")
    for (el <- elements) {
      el.className() match {
        case "full" | "paragraph" => el.unwrap()
      }
    }
    el
  }
}
