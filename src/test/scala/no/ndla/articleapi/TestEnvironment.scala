/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi

import com.amazonaws.services.s3.AmazonS3Client
import io.searchbox.client.JestClient
import no.ndla.articleapi.controller.{ArticleController, HealthController, InternController}
import no.ndla.articleapi.integration._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.converters.contentbrowser._
import no.ndla.articleapi.service.converters._
import no.ndla.articleapi.service.search.{IndexService, SearchConverterService, SearchIndexService, SearchService}
import no.ndla.network.NdlaClient
import org.scalatest.mockito.MockitoSugar

trait TestEnvironment
  extends ElasticClient
  with SearchService
  with IndexService
  with SearchIndexService
  with ArticleController
  with InternController
  with HealthController
  with DataSource
  with ArticleRepository
  with MockitoSugar
  with MigrationApiClient
  with ExtractService
  with ConverterModules
  with ConverterService
  with ContentBrowserConverterModules
  with ContentBrowserConverter
  with BiblioConverterModule
  with BiblioConverter
  with AmazonClient
  with StorageService
  with ArticleContentInformation
  with ExtractConvertStoreContent
  with NdlaClient
  with TagsService
  with SearchConverterService
  with ReadService
  with HtmlTagGenerator
  with HTMLCleaner
  with SequenceGenerator
{

  val searchService = mock[SearchService]
  val indexService = mock[IndexService]
  val searchIndexService = mock[SearchIndexService]

  val internController = mock[InternController]
  val articleController = mock[ArticleController]
  val healthController = mock[HealthController]

  val dataSource = mock[javax.sql.DataSource]
  val articleRepository = mock[ArticleRepository]
  val amazonClient = mock[AmazonS3Client]
  val storageName = "testStorageName"

  val extractConvertStoreContent = mock[ExtractConvertStoreContent]

  val migrationApiClient = mock[MigrationApiClient]
  val extractService = mock[ExtractService]

  val converterService = new ConverterService
  val contentBrowserConverter = new ContentBrowserConverter
  val biblioConverter = new BiblioConverter
  val htmlCleaner = new HTMLCleaner
  val converterModules = List(contentBrowserConverter)
  val postProcessorModules = List(SimpleTagConverter, biblioConverter, DivTableConverter, TableConverter, htmlCleaner)
  val storageService = mock[AmazonStorageService]
  val readService = mock[ReadService]

  val ndlaClient = mock[NdlaClient]
  val tagsService = mock[TagsService]
  val searchConverterService = mock[SearchConverterService]
  val jestClient = mock[JestClient]
  val audioApiClient = mock[AudioApiClient]
  val imageApiClient = mock[ImageApiClient]

  override def nextNumberInSequence = "1"
}
