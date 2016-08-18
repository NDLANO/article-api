package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.integration.{AmazonClientComponent, CMDataComponent, DataSourceComponent, MappingApiClient}
import no.ndla.contentapi.model.{ImportStatus, RequiredLibrary}
import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service._
import no.ndla.network.NdlaClient


trait ContentBrowserConverterModule {
  def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus)
  val typeName: String
}

trait ContentBrowserConverterModules
  extends ExtractServiceComponent
  with CMDataComponent
  with StorageService
  with AmazonClientComponent
  with ImageApiServiceComponent
  with ConverterModules
  with ConverterServiceComponent
  with DataSourceComponent
  with ContentRepositoryComponent
  with ExtractConvertStoreContent
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
  with BiblioConverterModule
  with TagsService
  with MappingApiClient
  with NdlaClient
