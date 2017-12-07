/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.validation.ValidationException
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import scalikejdbc.DBSession

import scala.util.{Failure, Success, Try}

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override val converterService = new ConverterService

  val today = DateTime.now().toDate
  val yesterday = DateTime.now().minusDays(1).toDate
  val service = new WriteService()

  val articleId = 13
  val article: Article = TestData.sampleArticleWithPublicDomain.copy(id = Some(articleId), created = yesterday, updated = yesterday)

  override def beforeEach() = {
    Mockito.reset(articleIndexService, articleRepository)

    when(articleRepository.withId(articleId)).thenReturn(Option(article))
    when(articleIndexService.indexDocument(any[Article])).thenAnswer((invocation: InvocationOnMock) => Try(invocation.getArgumentAt(0, article.getClass)))
    when(readService.addUrlsOnEmbedResources(any[Article])).thenAnswer((invocation: InvocationOnMock) => invocation.getArgumentAt(0, article.getClass))
    when(articleRepository.getExternalIdFromId(any[Long])(any[DBSession])).thenReturn(Option("1234"))
    when(authUser.userOrClientid()).thenReturn("ndalId54321")
    when(clock.now()).thenReturn(today)
    when(contentValidator.validateArticle(any[Article], any[Boolean])).thenAnswer((invocation: InvocationOnMock) =>
      Success(invocation.getArgumentAt(0, classOf[Article]))
    )
    when(articleRepository.updateArticle(any[Article])(any[DBSession])).thenAnswer((invocation: InvocationOnMock) => {
      val arg = invocation.getArgumentAt(0, classOf[Article])
      Try(arg.copy(revision = Some(arg.revision.get + 1)))
    })
  }

  test("newArticleV2 should insert a given articleV2") {
    when(articleRepository.newArticle(any[Article])(any[DBSession])).thenReturn(article)
    when(articleRepository.getExternalIdFromId(any[Long])(any[DBSession])).thenReturn(None)

    service.newArticleV2(TestData.newArticleV2).get.id.toString should equal(article.id.get.toString)
    verify(articleRepository, times(1)).newArticle(any[Article])
    verify(articleIndexService, times(1)).indexDocument(any[Article])
  }

  test("That updateArticleV2 updates only content properly") {
    val newContent = "NyContentTest"
    val updatedApiArticle = api.UpdatedArticleV2(1, "en", None, Some(newContent), Seq(), None, None, None, None, None, Seq(), None)
    val expectedArticle = article.copy(revision = Some(article.revision.get + 1), content = Seq(ArticleContent(newContent, "en")), updated = today)

    service.updateArticleV2(articleId, updatedApiArticle).get should equal(converterService.toApiArticleV2(expectedArticle, "en").get)
  }

  test("That updateArticleV2 updates only title properly") {
    val newTitle = "NyTittelTest"
    val updatedApiArticle = api.UpdatedArticleV2(1, "en", Some(newTitle), None, Seq(), None, None, None, None, None, Seq(), None)
    val expectedArticle = article.copy(revision = Some(article.revision.get + 1), title = Seq(ArticleTitle(newTitle, "en")), updated = today)

    service.updateArticleV2(articleId, updatedApiArticle).get should equal(converterService.toApiArticleV2(expectedArticle, "en").get)
  }

  test("That updateArticleV2 updates multiple fields properly") {
    val updatedTitle = "NyTittelTest"
    val updatedContent = "NyContentTest"
    val updatedTags = Seq("en", "to", "tre")
    val updatedMetaDescription = "updatedMetaHere"
    val updatedIntro = "introintro"
    val updatedMetaId = "1234"
    val updatedVisualElement = "<embed something>"
    val updatedCopyright = api.Copyright(api.License("a", Some("b"), None), "c", Seq(api.Author("Opphavsmann", "Jonas")), Seq(), Seq(), None, None, None)
    val updatedRequiredLib = api.RequiredLibrary("tjup", "tjap", "tjim")

    val updatedApiArticle = api.UpdatedArticleV2(1, "en", Some(updatedTitle), Some(updatedContent), updatedTags,
      Some(updatedIntro), Some(updatedMetaDescription), Some(updatedMetaId), Some(updatedVisualElement),
      Some(updatedCopyright), Seq(updatedRequiredLib), None)

    val expectedArticle = article.copy(
      revision = Some(article.revision.get + 1),
      title = Seq(ArticleTitle(updatedTitle, "en")),
      content = Seq(ArticleContent(updatedContent, "en")),
      copyright = Copyright("a", "c", Seq(Author("Opphavsmann", "Jonas")), Seq(), Seq(), None, None, None),
      tags = Seq(ArticleTag(Seq("en", "to", "tre"), "en")),
      requiredLibraries = Seq(RequiredLibrary("tjup", "tjap", "tjim")),
      visualElement = Seq(VisualElement(updatedVisualElement, "en")),
      introduction = Seq(ArticleIntroduction(updatedIntro, "en")),
      metaDescription = Seq(ArticleMetaDescription(updatedMetaDescription, "en")),
      metaImageId = Some(updatedMetaId),
      updated = today)

    service.updateArticleV2(articleId, updatedApiArticle).get should equal(converterService.toApiArticleV2(expectedArticle, "en").get)
  }

  test("allocateArticleId should reuse existing id if external id already exists") {
    val id = 1122: Long
    when(articleRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(Some(id))
    service.allocateArticleId(Some("123123123"), Set.empty) should equal(id)
  }

  test("allocateArticleId should allocate new id if no external id is supplied or first time use of external id") {
    val id = 1122: Long
    val external = "12312313"
    when(articleRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(None)
    when(articleRepository.allocateArticleIdWithExternal(any[String], any[Set[String]])(any[DBSession])).thenReturn(id)
    service.allocateArticleId(Some(external), Set.empty) should equal(id)
    verify(articleRepository, times(0)).allocateArticleId()
    verify(articleRepository, times(1)).allocateArticleIdWithExternal(external, Set.empty)

    reset(articleRepository)
    when(articleRepository.allocateArticleId()(any[DBSession])).thenReturn(id)
    service.allocateArticleId(None, Set.empty) should equal(id)
    verify(articleRepository, times(1)).allocateArticleId()
    verify(articleRepository, times(0)).allocateArticleIdWithExternal(external, Set.empty)
  }

  test("allocateConceptId should reuse existing id if external id already exists") {
    val id = 1122: Long
    when(conceptRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(Some(id))
    service.allocateConceptId(Some("123123123")) should equal(id)
  }

  test("allocateConceptId should allocate new id if no external id is supplied or first time use of external id") {
    val id = 1122: Long
    val external = "12312313"
    when(conceptRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(None)
    when(conceptRepository.allocateConceptIdWithExternal(any[String])(any[DBSession])).thenReturn(id)
    service.allocateConceptId(Some(external)) should equal(id)
    verify(conceptRepository, times(0)).allocateConceptId()
    verify(conceptRepository, times(1)).allocateConceptIdWithExternal(external)

    reset(conceptRepository)
    when(conceptRepository.allocateConceptId()(any[DBSession])).thenReturn(id)
    service.allocateConceptId(None) should equal(id)
    verify(conceptRepository, times(1)).allocateConceptId()
    verify(conceptRepository, times(0)).allocateConceptIdWithExternal(external)
  }

  test("newConcept should return Success if everything went well") {
    when(importValidator.validate(any[Concept], any[Boolean]))
      .thenAnswer((invocation: InvocationOnMock) => Try(invocation.getArgumentAt(0, classOf[Concept])))
    when(conceptRepository.insert(any[Concept])(any[DBSession]))
      .thenAnswer((invocation: InvocationOnMock) => invocation.getArgumentAt(0, classOf[Concept]).copy(id=Some(1)))
    when(conceptIndexService.indexDocument(any[Concept]))
      .thenAnswer((invocation: InvocationOnMock) => Try(invocation.getArgumentAt(0, classOf[Concept])))

    val res = service.newConcept(TestData.sampleNewConcept)
    res.isSuccess should be (true)
    verify(importValidator, times(1)).validate(any[Concept], any[Boolean])
    verify(conceptRepository, times(1)).insert(any[Concept])(any[DBSession])
    verify(conceptIndexService, times(1)).indexDocument(any[Concept])
  }

  test("newConcept should return Failure if validate fails") {
    reset(importValidator, conceptRepository, conceptIndexService)
    when(importValidator.validate(any[Concept], any[Boolean])).thenReturn(Failure(new ValidationException("fail", Seq.empty)))

    val res = service.newConcept(TestData.sampleNewConcept)
    res.isFailure should be (true)
    verify(importValidator, times(1)).validate(any[Concept], any[Boolean])
    verify(conceptRepository, times(0)).insert(any[Concept])(any[DBSession])
    verify(conceptIndexService, times(0)).indexDocument(any[Concept])
  }

  test("updateConcept should return Success if everything went well") {
    reset(importValidator, conceptRepository, conceptIndexService)
    when(conceptRepository.withId(any[Long])).thenReturn(Some(TestData.sampleConcept))
    when(importValidator.validate(any[Concept], any[Boolean]))
      .thenAnswer((invocation: InvocationOnMock) => Try(invocation.getArgumentAt(0, classOf[Concept])))
    when(conceptRepository.update(any[Concept], any[Long])(any[DBSession]))
      .thenAnswer((invocation: InvocationOnMock) => Try(invocation.getArgumentAt(0, classOf[Concept])))
    when(conceptIndexService.indexDocument(any[Concept]))
      .thenAnswer((invocation: InvocationOnMock) => Try(invocation.getArgumentAt(0, classOf[Concept])))

    val res = service.updateConcept(1, TestData.sampleUpdateConcept)
    res.isSuccess should be (true)
    verify(importValidator, times(1)).validate(any[Concept], any[Boolean])
    verify(conceptRepository, times(1)).update(any[Concept], any[Long])(any[DBSession])
    verify(conceptIndexService, times(1)).indexDocument(any[Concept])
  }

  test("updateConcept should return Failure if validate fails") {
    reset(importValidator, conceptRepository, conceptIndexService)
    when(conceptRepository.withId(any[Long])).thenReturn(Some(TestData.sampleConcept))
    when(importValidator.validate(any[Concept], any[Boolean])).thenReturn(Failure(new ValidationException("fail", Seq.empty)))

    val res = service.updateConcept(1, TestData.sampleUpdateConcept)
    res.isFailure should be (true)
    verify(importValidator, times(1)).validate(any[Concept], any[Boolean])
    verify(conceptRepository, times(0)).insert(any[Concept])(any[DBSession])
    verify(conceptIndexService, times(0)).indexDocument(any[Concept])
  }

}
