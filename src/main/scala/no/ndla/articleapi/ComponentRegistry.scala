/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.searchbox.client.JestClient
import no.ndla.articleapi.controller.{ArticleController, HealthController, InternController}
import no.ndla.articleapi.integration._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.converters._
import no.ndla.articleapi.service.converters.contentbrowser._
import no.ndla.articleapi.service.search.{IndexService, SearchConverterService, SearchIndexService, SearchService}
import no.ndla.network.NdlaClient
import org.postgresql.ds.PGPoolingDataSource

object ComponentRegistry
  extends DataSource
  with InternController
  with ArticleController
  with HealthController
  with ArticleRepository
  with ElasticClient
  with SearchService
  with IndexService
  with SearchIndexService
  with ExtractService
  with ConverterModules
  with ConverterService
  with ContentBrowserConverterModules
  with ContentBrowserConverter
  with BiblioConverterModule
  with BiblioConverter
  with AmazonClient
  with AttachmentStorageService
  with ArticleContentInformation
  with ExtractConvertStoreContent
  with NdlaClient
  with TagsService
  with MigrationApiClient
  with SearchConverterService
  with ReadService
  with HTMLCleaner
  with HtmlTagGenerator
  with SequenceGenerator
{
  implicit val swagger = new ArticleSwagger

  lazy val dataSource = new PGPoolingDataSource()
  dataSource.setUser(ArticleApiProperties.MetaUserName)
  dataSource.setPassword(ArticleApiProperties.MetaPassword)
  dataSource.setDatabaseName(ArticleApiProperties.MetaResource)
  dataSource.setServerName(ArticleApiProperties.MetaServer)
  dataSource.setPortNumber(ArticleApiProperties.MetaPort)
  dataSource.setInitialConnections(ArticleApiProperties.MetaInitialConnections)
  dataSource.setMaxConnections(ArticleApiProperties.MetaMaxConnections)
  dataSource.setCurrentSchema(ArticleApiProperties.MetaSchema)

  lazy val extractConvertStoreContent = new ExtractConvertStoreContent
  lazy val internController = new InternController
  lazy val articleController = new ArticleController
  lazy val resourcesApp = new ResourcesApp
  lazy val healthController = new HealthController

  lazy val articleRepository = new ArticleRepository
  lazy val searchService = new SearchService
  lazy val indexService = new IndexService
  lazy val searchIndexService = new SearchIndexService

  val amazonClient = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build()
  lazy val attachmentStorageName = ArticleApiProperties.AttachmentStorageName
  lazy val attachmentStorageService = new AmazonStorageService

  lazy val migrationApiClient = new MigrationApiClient
  lazy val extractService = new ExtractService
  lazy val converterService = new ConverterService

  lazy val ndlaClient = new NdlaClient
  lazy val tagsService = new TagsService
  lazy val searchConverterService = new SearchConverterService
  lazy val readService = new ReadService
  lazy val contentBrowserConverter = new ContentBrowserConverter
  lazy val biblioConverter = new BiblioConverter
  lazy val htmlCleaner = new HTMLCleaner
  lazy val converterModules = List(contentBrowserConverter)
  lazy val postProcessorModules = List(SimpleTagConverter, biblioConverter, DivTableConverter, TableConverter, htmlCleaner)

  lazy val jestClient: NdlaJestClient = JestClientFactory.getClient()
  lazy val audioApiClient = new AudioApiClient
  lazy val imageApiClient = new ImageApiClient
}
