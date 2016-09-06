/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi

import javax.sql.DataSource

import com.amazonaws.services.s3.AmazonS3Client
import com.sksamuel.elastic4s.ElasticClient
import no.ndla.articleapi.controller.{ArticleController, InternController}
import no.ndla.articleapi.integration._
import no.ndla.articleapi.repository.ArticleRepositoryComponent
import no.ndla.articleapi.service.converters.{BiblioConverter, DivTableConverter, SimpleTagConverter}
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.converters.contentbrowser._
import no.ndla.articleapi.service.search.{ElasticContentIndexComponent, ElasticContentSearchComponent, SearchIndexServiceComponent}
import no.ndla.network.NdlaClient
import org.scalatest.mock.MockitoSugar


trait TestEnvironment
  extends ElasticClientComponent
  with ElasticContentSearchComponent
  with ElasticContentIndexComponent
  with SearchIndexServiceComponent
  with ArticleController
  with InternController
  with DataSourceComponent
  with ArticleRepositoryComponent
  with MockitoSugar
  with MigrationApiClient
  with ExtractServiceComponent
  with ConverterModules
  with ConverterServiceComponent
  with ContentBrowserConverterModules
  with ContentBrowserConverter
  with BiblioConverterModule
  with BiblioConverter
  with ImageApiServiceComponent
  with AmazonClientComponent
  with StorageService
  with HtmlTagsUsage
  with ExtractConvertStoreContent
  with NdlaClient
  with MappingApiClient
  with TagsService
{
  val elasticClient = mock[ElasticClient]
  val elasticContentSearch = mock[ElasticContentSearch]
  val elasticContentIndex = mock[ElasticContentIndex]
  val searchIndexService = mock[SearchIndexService]

  val internController = mock[InternController]
  val articleController = mock[ArticleController]

  val dataSource = mock[DataSource]
  val articleRepository = mock[ArticleRepository]
  val amazonClient = mock[AmazonS3Client]
  val storageName = "testStorageName"

  val extractConvertStoreContent = mock[ExtractConvertStoreContent]

  val migrationApiClient = mock[MigrationApiClient]
  val extractService = mock[ExtractService]
  val converterService = new ConverterService
  val contentBrowserConverter = new ContentBrowserConverter
  val biblioConverter = new BiblioConverter
  val converterModules = List(SimpleTagConverter, biblioConverter, DivTableConverter, contentBrowserConverter)
  val imageApiService = mock[ImageApiService]
  val storageService = mock[AmazonStorageService]
  val ndlaClient = mock[NdlaClient]
  val mappingApiClient = mock[MappingApiClient]
  val tagsService = mock[TagsService]
}
