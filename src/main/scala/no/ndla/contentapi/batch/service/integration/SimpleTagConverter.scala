package no.ndla.contentapi.batch.service.integration

import scala.collection.JavaConversions._
import org.jsoup.nodes.Element

object SimpleTagConverter extends ConverterModule {

  def convert(doc: Element) {
    var elements = doc.select("div")
    for (el <- elements) {
      el.className() match {
        case "full" | "paragraph" => el.unwrap()
      }
    }
  }
}
