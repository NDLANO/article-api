package no.ndla.contentapi.batch.integration

import org.jsoup.nodes.Element

trait ConverterModule {
  def convert(el: Element): Element
}

