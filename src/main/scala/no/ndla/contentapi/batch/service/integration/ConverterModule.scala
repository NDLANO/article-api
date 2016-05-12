package no.ndla.contentapi.batch.service.integration

import org.jsoup.nodes.Element

trait ConverterModule {
  def convert(doc: Element)
}

