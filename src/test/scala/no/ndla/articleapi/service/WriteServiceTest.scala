/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import scalikejdbc.DBSession

import scala.util.{Success, Try}

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override val converterService = new ConverterService

  val today = DateTime.now().toDate
  val yesterday = DateTime.now().minusDays(1).toDate
  val service = new WriteService()

  val articleId = 13

  val article: Article =
    TestData.sampleArticleWithPublicDomain.copy(id = Some(articleId), created = yesterday, updated = yesterday)

  override def beforeEach() = {
    Mockito.reset(articleIndexService, articleRepository)

    when(articleRepository.withId(articleId)).thenReturn(Option(article))
    when(articleIndexService.indexDocument(any[Article])).thenAnswer((invocation: InvocationOnMock) =>
      Try(invocation.getArgumentAt(0, article.getClass)))
    when(readService.addUrlsOnEmbedResources(any[Article])).thenAnswer((invocation: InvocationOnMock) =>
      invocation.getArgumentAt(0, article.getClass))
    when(articleRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List("1234"))
    when(authUser.userOrClientid()).thenReturn("ndalId54321")
    when(clock.now()).thenReturn(today)
    when(contentValidator.validateArticle(any[Article], any[Boolean], any[Boolean]))
      .thenAnswer((invocation: InvocationOnMock) => Success(invocation.getArgumentAt(0, classOf[Article])))
  }

  test("allocateArticleId should reuse existing id if external id already exists") {
    val id = 1122: Long
    when(articleRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(Some(id))
    service.allocateArticleId(List("123123123"), Set.empty) should equal(id)
  }

  test("allocateArticleId should allocate new id if no external id is supplied or first time use of external id") {
    val id = 1122: Long
    val external = "12312313"
    when(articleRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(None)
    when(articleRepository.allocateArticleIdWithExternalIds(any[List[String]], any[Set[String]])(any[DBSession]))
      .thenReturn(id)
    service.allocateArticleId(List(external), Set.empty) should equal(id)
    verify(articleRepository, times(0)).allocateArticleId()
    verify(articleRepository, times(1)).allocateArticleIdWithExternalIds(List(external), Set.empty)

    reset(articleRepository)
    when(articleRepository.allocateArticleId()(any[DBSession])).thenReturn(id)
    service.allocateArticleId(List.empty, Set.empty) should equal(id)
    verify(articleRepository, times(1)).allocateArticleId()
    verify(articleRepository, times(0)).allocateArticleIdWithExternalIds(List(external), Set.empty)
  }

  test("allocateConceptId should reuse existing id if external id already exists") {
    val id = 1122: Long
    when(conceptRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(Some(id))
    service.allocateConceptId(List("123123123")) should equal(id)
  }

  test("allocateConceptId should allocate new id if no external id is supplied or first time use of external id") {
    val id = 1122: Long
    val external = "12312313"
    when(conceptRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(None)
    when(conceptRepository.allocateConceptIdWithExternalIds(any[List[String]])(any[DBSession])).thenReturn(id)
    service.allocateConceptId(List(external)) should equal(id)
    verify(conceptRepository, times(0)).allocateConceptId()
    verify(conceptRepository, times(1)).allocateConceptIdWithExternalIds(List(external))

    reset(conceptRepository)
    when(conceptRepository.allocateConceptId()(any[DBSession])).thenReturn(id)
    service.allocateConceptId(List.empty) should equal(id)
    verify(conceptRepository, times(1)).allocateConceptId()
    verify(conceptRepository, times(0)).allocateConceptIdWithExternalIds(List(external))
  }

}
