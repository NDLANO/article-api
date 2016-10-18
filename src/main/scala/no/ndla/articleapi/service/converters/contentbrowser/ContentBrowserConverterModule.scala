/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.integration._
import no.ndla.articleapi.model.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.repository.ArticleRepositoryComponent
import no.ndla.articleapi.service._
import no.ndla.network.NdlaClient


trait ContentBrowserConverterModule {
  def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus)
  val typeName: String
}

trait ContentBrowserConverterModules
  extends ExtractServiceComponent
  with StorageService
  with AmazonClientComponent
  with ConverterModules
  with ConverterServiceComponent
  with DataSourceComponent
  with ArticleRepositoryComponent
  with ExtractConvertStoreContent
  with ImageConverterModule
  with ImageApiClient
  with LenkeConverterModule
  with H5PConverterModule
  with OppgaveConverterModule
  with FagstoffConverterModule
  with NonExistentNodeConverterModule
  with AudioConverterModule
  with AudioApiClient
  with AktualitetConverterModule
  with VideoConverterModule
  with FilConverterModule
  with VeiledningConverterModule
  with BiblioConverterModule
  with TagsService
  with MappingApiClient
  with NdlaClient
  with MigrationApiClient
