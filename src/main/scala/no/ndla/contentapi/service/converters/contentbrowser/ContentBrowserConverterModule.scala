package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.integration.{AmazonClientComponent, CMDataComponent}
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.{ExtractServiceComponent, ImageApiServiceComponent, StorageService}


trait ContentBrowserConverterModule {
  def convert(content: ContentBrowser): (String, Seq[RequiredLibrary], Seq[String])
  val typeName: String
}

trait ContentBrowserConverterModules
  extends ExtractServiceComponent
  with CMDataComponent
  with StorageService
  with AmazonClientComponent
  with ImageApiServiceComponent
  with ImageConverterModule
  with LenkeConverterModule
  with H5PConverterModule
  with OppgaveConverterModule
  with FagstoffConverterModule
  with NonExistentNodeConverterModule
  with AudioConverterModule
  with AktualitetConverterModule
  with VideoConverterModule
  with FilConverterModule
  with VeiledningConverterModule

