package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.model.RequiredLibrary


trait ContentBrowserConverterModule {
  def convert(content: ContentBrowser): (String, List[RequiredLibrary], List[String])
  val typeName: String
}
