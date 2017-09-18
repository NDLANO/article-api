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
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.auth.{Role, User}
import no.ndla.articleapi.controller.{ArticleController, ConceptController, HealthController, InternController}
import no.ndla.articleapi.controller.{ArticleController, ArticleControllerV2, HealthController, InternController}
import no.ndla.articleapi.integration._
import no.ndla.articleapi.repository.{ArticleRepository, ConceptRepository}
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.converters._
import no.ndla.articleapi.service.converters.contentbrowser._
import no.ndla.articleapi.service.search._
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import org.postgresql.ds.PGPoolingDataSource
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
  extends DataSource
    with InternController
    with ArticleController
    with ConceptController
    with ConceptSearchService
    with ConceptIndexService
    with ArticleControllerV2
    with HealthController
    with ArticleRepository
    with ConceptRepository
    with ElasticClient
    with ArticleSearchService
    with IndexService
    with ArticleIndexService
    with SearchService
    with LazyLogging
    with ExtractService
    with ConverterModules
    with ConverterService
    with LeafNodeConverter
    with ContentBrowserConverterModules
    with ContentBrowserConverter
    with BiblioConverterModule
    with BiblioConverter
    with VisualElementConverter
    with AmazonClient
    with AttachmentStorageService
    with ArticleContentInformation
    with ExtractConvertStoreContent
    with NdlaClient
    with TagsService
    with MigrationApiClient
    with SearchConverterService
    with ReadService
    with WriteService
    with ContentValidator
    with HTMLCleaner
    with HtmlTagGenerator
    with Clock
    with Role
    with User {

  implicit val swagger = new ArticleSwagger

  lazy val dataSource = new PGPoolingDataSource
  dataSource.setUser(ArticleApiProperties.MetaUserName)
  dataSource.setPassword(ArticleApiProperties.MetaPassword)
  dataSource.setDatabaseName(ArticleApiProperties.MetaResource)
  dataSource.setServerName(ArticleApiProperties.MetaServer)
  dataSource.setPortNumber(ArticleApiProperties.MetaPort)
  dataSource.setInitialConnections(ArticleApiProperties.MetaInitialConnections)
  dataSource.setMaxConnections(ArticleApiProperties.MetaMaxConnections)
  dataSource.setCurrentSchema(ArticleApiProperties.MetaSchema)
  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  lazy val extractConvertStoreContent = new ExtractConvertStoreContent
  lazy val internController = new InternController
  lazy val articleController = new ArticleController
  lazy val articleControllerV2 = new ArticleControllerV2
  lazy val conceptController = new ConceptController
  lazy val resourcesApp = new ResourcesApp
  lazy val healthController = new HealthController

  lazy val articleRepository = new ArticleRepository
  lazy val conceptRepository = new ConceptRepository
  lazy val articleSearchService = new ArticleSearchService
  lazy val articleIndexService = new ArticleIndexService
  lazy val conceptSearchService = new ConceptSearchService
  lazy val conceptIndexService = new ConceptIndexService

  val amazonClient = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build()
  lazy val attachmentStorageName = ArticleApiProperties.AttachmentStorageName
  lazy val attachmentStorageService = new AmazonStorageService

  lazy val migrationApiClient = new MigrationApiClient
  lazy val extractService = new ExtractService
  lazy val converterService = new ConverterService
  lazy val contentValidator = new ContentValidator(allowEmptyLanguageField = false)
  lazy val importValidator = new ContentValidator(allowEmptyLanguageField = true)

  lazy val ndlaClient = new NdlaClient
  lazy val tagsService = new TagsService
  lazy val searchConverterService = new SearchConverterService
  lazy val readService = new ReadService
  lazy val writeService = new WriteService
  lazy val contentBrowserConverter = new ContentBrowserConverter
  lazy val biblioConverter = new BiblioConverter
  lazy val htmlCleaner = new HTMLCleaner

  override lazy val articleConverter = ConverterPipeLine(
    mainConverters = List(contentBrowserConverter),
    postProcessorConverters = List(LeafNodeConverter, biblioConverter, TableConverter, MathMLConverter, htmlCleaner, VisualElementConverter)
  )
  override lazy val conceptConverter = ConverterPipeLine(
    mainConverters = List(contentBrowserConverter),
    postProcessorConverters = List(ConceptConverter)
  )
  override lazy val leafNodeConverter = ConverterPipeLine(
    mainConverters = Seq.empty,
    postProcessorConverters = List(LeafNodeConverter, htmlCleaner)
  )

  lazy val jestClient: NdlaJestClient = JestClientFactory.getClient()
  lazy val audioApiClient = new AudioApiClient
  lazy val imageApiClient = new ImageApiClient

  lazy val clock = new SystemClock
  lazy val authRole = new AuthRole
  lazy val authUser = new AuthUser
}
