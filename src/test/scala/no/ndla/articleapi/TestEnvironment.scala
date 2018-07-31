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
import no.ndla.articleapi.controller._
import no.ndla.articleapi.integration._
import no.ndla.articleapi.repository.{ArticleRepository, ConceptRepository}
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.search._
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import org.scalatest.mockito.MockitoSugar

trait TestEnvironment
    extends Elastic4sClient
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
    with DraftApiClient
    with ConverterService
    with NdlaClient
    with SearchConverterService
    with ReadService
    with WriteService
    with ContentValidator
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

  val converterService = mock[ConverterService]
  val readService = mock[ReadService]
  val writeService = mock[WriteService]
  val contentValidator = mock[ContentValidator]

  val ndlaClient = mock[NdlaClient]
  val searchConverterService = mock[SearchConverterService]
  val e4sClient = mock[NdlaE4sClient]
  val draftApiClient = mock[DraftApiClient]

  val clock = mock[SystemClock]
  val authUser = mock[AuthUser]
  val authRole = new AuthRole
}
