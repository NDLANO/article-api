/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi

import com.amazonaws.services.s3.AmazonS3Client
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.auth.{Role, User}
import no.ndla.articleapi.controller.{ArticleController, HealthController, InternController}
import no.ndla.articleapi.integration._
import no.ndla.articleapi.repository.{ArticleRepository, ConceptRepository}
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.converters._
import no.ndla.articleapi.service.converters.contentbrowser._
import no.ndla.articleapi.service.search._
import no.ndla.articleapi.validation.ArticleValidator
import no.ndla.network.NdlaClient
import org.scalatest.mockito.MockitoSugar

trait TestEnvironment
  extends ElasticClient
    with ArticleSearchService
    with ArticleIndexService
    with IndexService
    with SearchService
    with LazyLogging
    with ArticleController
    with InternController
    with HealthController
    with DataSource
    with ArticleRepository
    with ConceptRepository
    with MockitoSugar
    with MigrationApiClient
    with ExtractService
    with ConverterModules
    with ConverterService
    with ContentBrowserConverterModules
    with ContentBrowserConverter
    with BiblioConverterModule
    with VisualElementConverter
    with BiblioConverter
    with AmazonClient
    with AttachmentStorageService
    with ArticleContentInformation
    with ExtractConvertStoreContent
    with NdlaClient
    with TagsService
    with SearchConverterService
    with ReadService
    with WriteService
    with ArticleValidator
    with HtmlTagGenerator
    with HTMLCleaner
    with Clock
    with User
    with Role {
  val articleSearchService = mock[ArticleSearchService]
  val articleIndexService = mock[ArticleIndexService]

  val internController = mock[InternController]
  val articleController = mock[ArticleController]
  val healthController = mock[HealthController]

  val dataSource = mock[javax.sql.DataSource]
  val articleRepository = mock[ArticleRepository]
  val conceptRepository = mock[ConceptRepository]
  val amazonClient = mock[AmazonS3Client]
  val attachmentStorageName = "testStorageName"

  val extractConvertStoreContent = mock[ExtractConvertStoreContent]

  val migrationApiClient = mock[MigrationApiClient]
  val extractService = mock[ExtractService]

  val converterService = mock[ConverterService]
  val contentBrowserConverter = new ContentBrowserConverter
  val biblioConverter = new BiblioConverter
  val htmlCleaner = new HTMLCleaner
  val articleConverterModules = List(contentBrowserConverter)
  val articlePostProcessorModules = List(SimpleTagConverter, biblioConverter, TableConverter, MathMLConverter, htmlCleaner, VisualElementConverter)
  val conceptConverterModules = List(contentBrowserConverter)
  val conceptPostProcessorModules = List(ConceptConverter)
  val attachmentStorageService = mock[AmazonStorageService]
  val readService = mock[ReadService]
  val writeService = mock[WriteService]
  val articleValidator = mock[ArticleValidator]
  val importValidator = mock[ArticleValidator]

  val ndlaClient = mock[NdlaClient]
  val tagsService = mock[TagsService]
  val searchConverterService = mock[SearchConverterService]
  val jestClient = mock[NdlaJestClient]
  val audioApiClient = mock[AudioApiClient]
  val imageApiClient = mock[ImageApiClient]

  val clock = mock[SystemClock]
  val authUser = mock[AuthUser]
  val authRole = new AuthRole
}
