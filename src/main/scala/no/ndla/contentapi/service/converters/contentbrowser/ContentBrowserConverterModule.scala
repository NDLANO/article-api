package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.model.RequiredLibrary


trait ContentBrowserConverterModule {
  def convert(content: ContentBrowser): (String, Seq[RequiredLibrary], Seq[String])
  val typeName: String
}
