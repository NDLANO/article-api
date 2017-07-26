/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.auth.User
import no.ndla.articleapi.integration._
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.repository.{ArticleRepository, ConceptRepository}
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.converters.HtmlTagGenerator
import no.ndla.articleapi.service.search._
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.network.NdlaClient

import scala.util.Try


trait ContentBrowserConverterModule {
  def convert(content: ContentBrowser, visitedNodes: Seq[String]): Try[(String, Seq[RequiredLibrary], ImportStatus)]
  val typeName: String
}

trait ContentBrowserConverterModules
  extends ExtractService
  with AttachmentStorageService
  with AmazonClient
  with ConverterModules
  with ConverterService
  with Clock
  with DataSource
  with ArticleRepository
  with ConceptRepository
  with ExtractConvertStoreContent
  with ArticleIndexService
  with ConceptIndexService
  with IndexService
  with LazyLogging
  with SearchService
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
  with BegrepConverterModule
  with TagsService
  with NdlaClient
  with MigrationApiClient
  with HtmlTagGenerator
  with UnsupportedContentConverter
  with User
  with ContentValidator
  with ReadService
