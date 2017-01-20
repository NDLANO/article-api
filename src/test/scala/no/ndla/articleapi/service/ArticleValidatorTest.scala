/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.api.ValidationException
import no.ndla.articleapi.model.domain.{ArticleContent, ArticleIntroduction, ArticleMetaDescription, ArticleTitle}
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}

class ArticleValidatorTest extends UnitSuite with TestEnvironment {
  override val validationService = new ArticleValidator
  val validDocument = """<h1>heisann</h1><h2>heia</h2>"""
  val invalidDocument = """<article><invalid></invalid></article>"""

  test("validateArticle does not throw an exception on a valid document") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, Some("nb"))))
    noException should be thrownBy validationService.validateArticle(article)
  }

  test("validateArticle throws a validation exception on an invalid document") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(invalidDocument, None, Some("nb"))))
    a [ValidationException] should be thrownBy validationService.validateArticle(article)
  }

  test("validateArticle should throw an error if introduction contains HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, Some("nb"))), introduction=Seq(ArticleIntroduction(validDocument, Some("nb"))))
    a [ValidationException] should be thrownBy validationService.validateArticle(article)
  }

  test("validateArticle should throw an error if metaDescription contains HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, Some("nb"))), metaDescription=Seq(ArticleMetaDescription(validDocument, Some("nb"))))
    a [ValidationException] should be thrownBy validationService.validateArticle(article)
  }

  test("validateArticle should throw an error if title contains HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, Some("nb"))), title=Seq(ArticleTitle(validDocument, Some("nb"))))
    a [ValidationException] should be thrownBy validationService.validateArticle(article)
  }
}
