/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.integration._
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.search.{IndexService, SearchConverterService}
import no.ndla.network.NdlaClient


trait ContentBrowserConverterModule {
  def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus)
  val typeName: String
}

trait ContentBrowserConverterModules
  extends ExtractService
  with StorageService
  with AmazonClient
  with ConverterModules
  with ConverterService
  with DataSource
  with ArticleRepository
  with ExtractConvertStoreContent
  with IndexService
  with ElasticClient
  with SearchConverterService
  with ImageConverterModule
  with ImageApiClient
  with LenkeConverterModule
  with H5PConverterModule
  with JoubelH5PConverterModule
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
  with UnsupportedContentConverter
