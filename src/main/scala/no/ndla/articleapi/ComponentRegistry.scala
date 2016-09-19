/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import no.ndla.articleapi.controller.{ArticleController, InternController}
import no.ndla.articleapi.integration._
import no.ndla.articleapi.repository.ArticleRepositoryComponent
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.converters.{BiblioConverter, DivTableConverter, HTMLCleaner, SimpleTagConverter}
import org.elasticsearch.common.settings.Settings
import no.ndla.articleapi.service.converters.contentbrowser._
import no.ndla.articleapi.service.search.{ElasticContentIndexComponent, SearchConverterService, SearchIndexServiceComponent, SearchService}
import no.ndla.network.NdlaClient
import org.postgresql.ds.PGPoolingDataSource

object ComponentRegistry
  extends DataSourceComponent
  with InternController
  with ArticleController
  with ArticleRepositoryComponent
  with ElasticClientComponent
  with SearchService
  with ElasticContentIndexComponent
  with SearchIndexServiceComponent
  with ExtractServiceComponent
  with ConverterModules
  with ConverterServiceComponent
  with ImageApiServiceComponent
  with ContentBrowserConverterModules
  with ContentBrowserConverter
  with BiblioConverterModule
  with BiblioConverter
  with AmazonClientComponent
  with StorageService
  with HtmlTagsUsage
  with ExtractConvertStoreContent
  with NdlaClient
  with MappingApiClient
  with TagsService
  with MigrationApiClient
  with SearchConverterService
{
  implicit val swagger = new ArticleSwagger

  lazy val dataSource = new PGPoolingDataSource()
  dataSource.setUser(ArticleApiProperties.get("META_USER_NAME"))
  dataSource.setPassword(ArticleApiProperties.get("META_PASSWORD"))
  dataSource.setDatabaseName(ArticleApiProperties.get("META_RESOURCE"))
  dataSource.setServerName(ArticleApiProperties.get("META_SERVER"))
  dataSource.setPortNumber(ArticleApiProperties.getInt("META_PORT"))
  dataSource.setInitialConnections(ArticleApiProperties.getInt("META_INITIAL_CONNECTIONS"))
  dataSource.setMaxConnections(ArticleApiProperties.getInt("META_MAX_CONNECTIONS"))
  dataSource.setCurrentSchema(ArticleApiProperties.get("META_SCHEMA"))

  lazy val extractConvertStoreContent = new ExtractConvertStoreContent
  lazy val internController = new InternController
  lazy val articleController = new ArticleController
  lazy val resourcesApp = new ResourcesApp

  lazy val elasticClient = ElasticClient.transport(
    Settings.settingsBuilder().put("cluster.name", ArticleApiProperties.SearchClusterName).build(),
    ElasticsearchClientUri(s"elasticsearch://${ArticleApiProperties.SearchHost}:${ArticleApiProperties.SearchPort}"))

  lazy val articleRepository = new ArticleRepository
  lazy val searchService = new SearchService
  lazy val elasticContentIndex = new ElasticContentIndex
  lazy val searchIndexService = new SearchIndexService

  val amazonClient = new AmazonS3Client(new BasicAWSCredentials(ArticleApiProperties.StorageAccessKey, ArticleApiProperties.StorageSecretKey))
  amazonClient.setRegion(Region.getRegion(Regions.EU_CENTRAL_1))
  lazy val storageName = ArticleApiProperties.StorageName
  lazy val storageService = new AmazonStorageService

  lazy val CMHost = ArticleApiProperties.CMHost
  lazy val CMPort = ArticleApiProperties.CMPort
  lazy val CMDatabase = ArticleApiProperties.CMDatabase
  lazy val CMUser = ArticleApiProperties.CMUser
  lazy val CMPassword = ArticleApiProperties.CMPassword
  lazy val imageApiBaseUrl = ArticleApiProperties.imageApiBaseUrl

  lazy val migrationApiClient = new MigrationApiClient
  lazy val extractService = new ExtractService
  lazy val converterService = new ConverterService
  lazy val imageApiService = new ImageApiService

  lazy val ndlaClient = new NdlaClient
  lazy val mappingApiClient = new MappingApiClient
  lazy val tagsService = new TagsService
  lazy val searchConverterService = new SearchConverterService

  lazy val contentBrowserConverter = new ContentBrowserConverter
  lazy val biblioConverter = new BiblioConverter
  lazy val converterModules = List(SimpleTagConverter, biblioConverter, DivTableConverter, contentBrowserConverter)
  lazy val postProcessorModules = List(HTMLCleaner)
}
