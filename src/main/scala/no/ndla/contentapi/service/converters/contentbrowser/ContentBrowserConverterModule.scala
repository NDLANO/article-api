package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.integration.CMDataComponent
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.{ExtractServiceComponent, ImageApiServiceComponent}


trait ContentBrowserConverterModule {
  def convert(content: ContentBrowser): (String, Seq[RequiredLibrary], Seq[String])
  val typeName: String
}

trait ContentBrowserConverterModules extends ExtractServiceComponent with CMDataComponent with ImageApiServiceComponent with ImageConverterModule with LenkeConverterModule with H5PConverterModule with OppgaveConverterModule with FagstoffConverterModule with NonExistentNodeConverterModule
