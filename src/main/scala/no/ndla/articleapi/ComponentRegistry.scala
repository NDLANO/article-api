/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.auth.{Role, User}
import no.ndla.articleapi.controller.{ArticleControllerV2, ConceptController, HealthController, InternController}
import no.ndla.articleapi.integration._
import no.ndla.articleapi.repository.{ArticleRepository, ConceptRepository}
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.search._
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import org.postgresql.ds.PGPoolingDataSource
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
  extends DataSource
    with InternController
    with ConceptController
    with ConceptSearchService
    with ConceptIndexService
    with ArticleControllerV2
    with HealthController
    with ArticleRepository
    with ConceptRepository
    with Elastic4sClient
    with DraftApiClient
    with ArticleSearchService
    with IndexService
    with ArticleIndexService
    with SearchService
    with LazyLogging
    with ConverterService
    with NdlaClient
    with SearchConverterService
    with ReadService
    with WriteService
    with ContentValidator
    with Clock
    with Role
    with User {

  implicit val swagger: ArticleSwagger = new ArticleSwagger

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

  lazy val internController = new InternController
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

  lazy val converterService = new ConverterService
  lazy val contentValidator = new ContentValidator(allowEmptyLanguageField = false)

  lazy val ndlaClient = new NdlaClient
  lazy val searchConverterService = new SearchConverterService
  lazy val readService = new ReadService
  lazy val writeService = new WriteService

  lazy val e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient()
  lazy val draftApiClient = new DraftApiClient

  lazy val clock = new SystemClock
  lazy val authRole = new AuthRole
  lazy val authUser = new AuthUser
}
