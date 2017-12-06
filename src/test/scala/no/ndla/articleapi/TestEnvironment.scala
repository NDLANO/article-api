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
import no.ndla.articleapi.controller._
import no.ndla.articleapi.integration._
import no.ndla.articleapi.repository.{ArticleRepository, ConceptRepository}
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.converters._
import no.ndla.articleapi.service.converters.contentbrowser._
import no.ndla.articleapi.service.search._
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import org.scalatest.mockito.MockitoSugar

trait TestEnvironment
  extends ElasticClient
    with ArticleSearchService
    with ArticleIndexService
    with ConceptSearchService
    with ConceptIndexService
    with IndexService
    with SearchService
    with LazyLogging
    with ArticleControllerV2
    with InternController
    with HealthController
    with ConceptController
    with DataSource
    with ArticleRepository
    with ConceptRepository
    with MockitoSugar
    with MigrationApiClient
    with ExtractService
    with ConverterModules
    with DraftApiClient
    with ConverterService
    with LeafNodeConverter
    with ContentBrowserConverterModules
    with ContentBrowserConverter
    with BiblioConverterModule
    with VisualElementConverter
    with RelatedContentConverter
    with AmazonClient
    with AttachmentStorageService
    with ArticleContentInformation
    with ExtractConvertStoreContent
    with NdlaClient
    with TagsService
    with SearchConverterService
    with ReadService
    with WriteService
    with ContentValidator
    with HtmlTagGenerator
    with HTMLCleaner
    with Clock
    with User
    with Role {
  val articleSearchService = mock[ArticleSearchService]
  val articleIndexService = mock[ArticleIndexService]
  val conceptSearchService = mock[ConceptSearchService]
  val conceptIndexService = mock[ConceptIndexService]

  val internController = mock[InternController]
  val articleControllerV2 = mock[ArticleControllerV2]
  val conceptController = mock[ConceptController]

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
  val htmlCleaner = new HTMLCleaner

  lazy val articleConverter = ConverterPipeLine(
    mainConverters = List(contentBrowserConverter),
    postProcessorConverters = List(SimpleTagConverter, TableConverter, MathMLConverter, RelatedContentConverter, htmlCleaner, VisualElementConverter)
  )
  lazy val conceptConverter = ConverterPipeLine(
    mainConverters = List(contentBrowserConverter),
    postProcessorConverters = List(ConceptConverter)
  )
  override lazy val leafNodeConverter = ConverterPipeLine(
    mainConverters = Seq(contentBrowserConverter),
    postProcessorConverters = List(LeafNodeConverter) ++ articleConverter.postProcessorConverters
  )
  val attachmentStorageService = mock[AmazonStorageService]
  val readService = mock[ReadService]
  val writeService = mock[WriteService]
  val contentValidator = mock[ContentValidator]
  val importValidator = mock[ContentValidator]

  val ndlaClient = mock[NdlaClient]
  val tagsService = mock[TagsService]
  val searchConverterService = mock[SearchConverterService]
  val jestClient = mock[NdlaJestClient]
  val audioApiClient = mock[AudioApiClient]
  val imageApiClient = mock[ImageApiClient]
  val draftApiClient = mock[DraftApiClient]

  val clock = mock[SystemClock]
  val authUser = mock[AuthUser]
  val authRole = new AuthRole
}
