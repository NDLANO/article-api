/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.domain.{Article, ArticleContent, ArticleTitle}
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.mockito.Mockito
import scalikejdbc.DBSession

import scala.util.Success

class WriteServiceTest extends UnitSuite with TestEnvironment {
  val today = DateTime.now().toDate
  val yesterday = DateTime.now().minusDays(1).toDate
  val service = new WriteService()

  val articleId = 13
  val article: Article = TestData.sampleArticleWithPublicDomain.copy(id=Some(articleId), created=yesterday, updated=yesterday)
  val updatedArticle = TestData.updatedArticle
  val newArticle = TestData.newArticle

  override def beforeEach() = {
    Mockito.reset(indexService, articleRepository)
  }

  test("newArticle should insert a given article") {
    when(articleRepository.insert(any[Article])(any[DBSession])).thenReturn(article)
    when(articleRepository.getExternalIdFromId(any[Long])(any[DBSession])).thenReturn(None)

    service.newArticle(newArticle).id should equal(article.id.get.toString)
    verify(articleRepository, times(1)).insert(any[Article])
    verify(indexService, times(1)).indexDocument(any[Article])
  }

  test("updateArticle should return Failure when trying to update a non-existing article") {
    when(articleRepository.withId(articleId)).thenReturn(None)
    service.updateArticle(articleId, updatedArticle).isFailure should equal(true)
    verify(articleRepository, times(0)).update(any[Article])
    verify(indexService, times(0)).indexDocument(any[Article])
  }

  test("updateArticle should update the updated field of an article") {
    val expectedUpdatedArticle = article.copy(updated=today)
    when(articleRepository.withId(articleId)).thenReturn(Some(article))
    when(articleRepository.update(any[Article])(any[DBSession])).thenReturn(Success(expectedUpdatedArticle))
    when(clock.now()).thenReturn(today)

    service.updateArticle(articleId, updatedArticle)
    verify(articleRepository, times(1)).update(expectedUpdatedArticle)
    verify(indexService, times(1)).indexDocument(any[Article])
  }


  test("That mergeLanguageFields returns original list when updated is empty") {
    val existing = Seq(ArticleTitle("Tittel 1", Some("nb")), ArticleTitle("Tittel 2", Some("nn")), ArticleTitle("Tittel 3", None))
    service.mergeLanguageFields(existing, Seq()) should equal (existing)
  }

  test("That mergeLanguageFields updated the english title only when specified") {
    val tittel1 = ArticleTitle("Tittel 1", Some("nb"))
    val tittel2 = ArticleTitle("Tittel 2", Some("nn"))
    val tittel3 = ArticleTitle("Tittel 3", Some("en"))
    val oppdatertTittel3 = ArticleTitle("Title 3 in english", Some("en"))

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(oppdatertTittel3)

    service.mergeLanguageFields(existing, updated) should equal (Seq(tittel1, tittel2, oppdatertTittel3))
  }

  test("That mergeLanguageFields removes a title that is empty") {
    val tittel1 = ArticleTitle("Tittel 1", Some("nb"))
    val tittel2 = ArticleTitle("Tittel 2", Some("nn"))
    val tittel3 = ArticleTitle("Tittel 3", Some("en"))
    val tittelToRemove = ArticleTitle("", Some("nn"))

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(tittelToRemove)

    service.mergeLanguageFields(existing, updated) should equal (Seq(tittel1, tittel3))
  }

  test("That mergeLanguageFields updates the title with no language specified") {
    val tittel1 = ArticleTitle("Tittel 1", Some("nb"))
    val tittel2 = ArticleTitle("Tittel 2", None)
    val tittel3 = ArticleTitle("Tittel 3", Some("en"))
    val oppdatertTittel2 = ArticleTitle("Tittel 2 er oppdatert", None)

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(oppdatertTittel2)

    service.mergeLanguageFields(existing, updated) should equal (Seq(tittel1, tittel3, oppdatertTittel2))
  }

  test("That mergeLanguageFields also updates the correct content") {
    val desc1 = ArticleContent("Beskrivelse 1", None, Some("nb"))
    val desc2 = ArticleContent("Beskrivelse 2", None, None)
    val desc3 = ArticleContent("Beskrivelse 3", None, Some("en"))
    val oppdatertDesc2 = ArticleContent("Beskrivelse 2 er oppdatert", None, None)

    val existing = Seq(desc1, desc2, desc3)
    val updated = Seq(oppdatertDesc2)

    service.mergeLanguageFields(existing, updated) should equal (Seq(desc1, desc3, oppdatertDesc2))
  }

}
