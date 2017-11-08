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
  val article: Article = TestData.sampleArticleWithPublicDomain.copy(id = Some(articleId), created = yesterday, updated = yesterday)

  override def beforeEach() = {
    Mockito.reset(articleIndexService, articleRepository)

    when(articleRepository.withId(articleId)).thenReturn(Option(article))
    when(articleIndexService.indexDocument(any[Article])).thenAnswer((invocation: InvocationOnMock) => Try(invocation.getArgumentAt(0, article.getClass)))
    when(readService.addUrlsOnEmbedResources(any[Article])).thenAnswer((invocation: InvocationOnMock) => invocation.getArgumentAt(0, article.getClass))
    when(contentValidator.validateArticle(any[Article], any[Boolean])).thenReturn(Success(article))
    when(articleRepository.getExternalIdFromId(any[Long])(any[DBSession])).thenReturn(Option("1234"))
    when(authUser.id()).thenReturn("ndalId54321")
    when(clock.now()).thenReturn(today)
    when(articleRepository.update(any[Article])(any[DBSession])).thenAnswer((invocation: InvocationOnMock) => {
      val arg = invocation.getArgumentAt(0, article.getClass)
      Try(arg.copy(revision = Some(arg.revision.get + 1)))
    })
  }

  test("newArticleV2 should insert a given articleV2") {
    when(articleRepository.insert(any[Article])(any[DBSession])).thenReturn(article)
    when(articleRepository.getExternalIdFromId(any[Long])(any[DBSession])).thenReturn(None)
    when(contentValidator.validateArticle(any[Article], any[Boolean])).thenReturn(Success(article))

    service.newArticleV2(TestData.newArticleV2).get.id.toString should equal(article.id.get.toString)
    verify(articleRepository, times(1)).insert(any[Article])
    verify(articleIndexService, times(1)).indexDocument(any[Article])
  }

  test("That mergeLanguageFields returns original list when updated is empty") {
    val existing = Seq(ArticleTitle("Tittel 1", "nb"), ArticleTitle("Tittel 2", "nn"), ArticleTitle("Tittel 3", "unknown"))
    service.mergeLanguageFields(existing, Seq()) should equal(existing)
  }

  test("That mergeLanguageFields updated the english title only when specified") {
    val tittel1 = ArticleTitle("Tittel 1", "nb")
    val tittel2 = ArticleTitle("Tittel 2", "nn")
    val tittel3 = ArticleTitle("Tittel 3", "en")
    val oppdatertTittel3 = ArticleTitle("Title 3 in english", "en")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(oppdatertTittel3)

    service.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel2, oppdatertTittel3))
  }

  test("That mergeLanguageFields removes a title that is empty") {
    val tittel1 = ArticleTitle("Tittel 1", "nb")
    val tittel2 = ArticleTitle("Tittel 2", "nn")
    val tittel3 = ArticleTitle("Tittel 3", "en")
    val tittelToRemove = ArticleTitle("", "nn")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(tittelToRemove)

    service.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel3))
  }

  test("That mergeLanguageFields updates the title with unknown language specified") {
    val tittel1 = ArticleTitle("Tittel 1", "nb")
    val tittel2 = ArticleTitle("Tittel 2", "unknown")
    val tittel3 = ArticleTitle("Tittel 3", "en")
    val oppdatertTittel2 = ArticleTitle("Tittel 2 er oppdatert", "unknown")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(oppdatertTittel2)

    service.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel3, oppdatertTittel2))
  }

  test("That mergeLanguageFields also updates the correct content") {
    val desc1 = ArticleContent("Beskrivelse 1", "nb")
    val desc2 = ArticleContent("Beskrivelse 2", "unknown")
    val desc3 = ArticleContent("Beskrivelse 3", "en")
    val oppdatertDesc2 = ArticleContent("Beskrivelse 2 er oppdatert", "unknown")

    val existing = Seq(desc1, desc2, desc3)
    val updated = Seq(oppdatertDesc2)

    service.mergeLanguageFields(existing, updated) should equal(Seq(desc1, desc3, oppdatertDesc2))
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
    val updatedCopyright = api.Copyright(api.License("a", Some("b"), None), "c", Seq(api.Author("Opphavsmann", "Jonas")), Seq(), Seq(), None, None)
    val updatedRequiredLib = api.RequiredLibrary("tjup", "tjap", "tjim")

    val updatedApiArticle = api.UpdatedArticleV2(1, "en", Some(updatedTitle), Some(updatedContent), updatedTags,
      Some(updatedIntro), Some(updatedMetaDescription), Some(updatedMetaId), Some(updatedVisualElement),
      Some(updatedCopyright), Seq(updatedRequiredLib), None)

    val expectedArticle = article.copy(
      revision = Some(article.revision.get + 1),
      title = Seq(ArticleTitle(updatedTitle, "en")),
      content = Seq(ArticleContent(updatedContent, "en")),
      copyright = Copyright("a", "c", Seq(Author("Opphavsmann", "Jonas")), Seq(), Seq(), None, None),
      tags = Seq(ArticleTag(Seq("en", "to", "tre"), "en")),
      requiredLibraries = Seq(RequiredLibrary("tjup", "tjap", "tjim")),
      visualElement = Seq(VisualElement(updatedVisualElement, "en")),
      introduction = Seq(ArticleIntroduction(updatedIntro, "en")),
      metaDescription = Seq(ArticleMetaDescription(updatedMetaDescription, "en")),
      metaImageId = Some(updatedMetaId),
      updated = today)

    service.updateArticleV2(articleId, updatedApiArticle).get should equal(converterService.toApiArticleV2(expectedArticle, "en").get)
  }

}
