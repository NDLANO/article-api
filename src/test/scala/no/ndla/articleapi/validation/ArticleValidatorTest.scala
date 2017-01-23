/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.validation

import no.ndla.articleapi.ArticleApiProperties.H5PResizerScriptUrl
import no.ndla.articleapi.model.api.ValidationException
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}

class ArticleValidatorTest extends UnitSuite with TestEnvironment {
  override val articleValidator = new ArticleValidator
  val validDocument = """<h1>heisann</h1><h2>heia</h2>"""
  val invalidDocument = """<article><invalid></invalid></article>"""

  test("validateArticle does not throw an exception on a valid document") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, Some("nb"))))
    noException should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle throws a validation exception on an invalid document") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(invalidDocument, None, Some("nb"))))
    a [ValidationException] should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle should throw an error if introduction contains HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, Some("nb"))), introduction=Seq(ArticleIntroduction(validDocument, Some("nb"))))
    a [ValidationException] should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle should not throw an error if introduction contains plain text") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, Some("nb"))), introduction=Seq(ArticleIntroduction("introduction", Some("nb"))))
    noException should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle should throw an error if metaDescription contains HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, Some("nb"))), metaDescription=Seq(ArticleMetaDescription(validDocument, Some("nb"))))
    a [ValidationException] should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle should not throw an error if metaDescription contains plain text") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, Some("nb"))), metaDescription=Seq(ArticleMetaDescription("meta description", Some("nb"))))
    noException should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle should throw an error if title contains HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, Some("nb"))), title=Seq(ArticleTitle(validDocument, Some("nb"))))
    a [ValidationException] should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle should not throw an error if title contains plain text") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, Some("nb"))), title=Seq(ArticleTitle("title", Some("nb"))))
    noException should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle throws a validation exception on an invalid visual element") {
    val invalidVisualElement = TestData.visualElement.copy(resource=invalidDocument)
    val article = TestData.sampleArticleWithByNcSa.copy(visualElement=Seq(invalidVisualElement))
    a [ValidationException] should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle does not throw an exception on a valid visual element") {
    val article = TestData.sampleArticleWithByNcSa.copy(visualElement=Seq(TestData.visualElement))
    noException should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle does not throw an exception on an article with plaintext tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(tags=Seq(ArticleTag(Seq("vann", "snø", "sol"), Some("nb"))))
    noException should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle throws an exception on an article with html in tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(tags=Seq(ArticleTag(Seq("<h1>vann</h1>", "snø", "sol"), Some("nb"))))
    a [ValidationException] should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle does not throw an exception on an article where metaImageId is a number") {
    val article = TestData.sampleArticleWithByNcSa.copy(metaImageId=Some("123"))
    noException should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle throws an exception on an article where metaImageId is not a number") {
    val article = TestData.sampleArticleWithByNcSa.copy(metaImageId=Some("not a number"))
    a [ValidationException] should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle throws an exception on an article with html in contentType") {
    val article = TestData.sampleArticleWithByNcSa.copy(contentType=validDocument)
    a [ValidationException] should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle does not throw an exception on an article with a plain text contentType") {
    val article = TestData.sampleArticleWithByNcSa.copy(contentType="fagstoff")
    noException should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle throws an exception on an article with an illegal required library") {
    val illegalRequiredLib = RequiredLibrary("text/javascript", "naughty", "http://scary.bad.source.net/notNice.js")
    val article = TestData.sampleArticleWithByNcSa.copy(requiredLibraries=Seq(illegalRequiredLib))
    a [ValidationException] should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle does not throw an exception on an article with a legal required library") {
    val illegalRequiredLib = RequiredLibrary("text/javascript", "h5p", H5PResizerScriptUrl)
    val article = TestData.sampleArticleWithByNcSa.copy(requiredLibraries=Seq(illegalRequiredLib))
    noException should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle throws an exception on an article with an invalid license") {
    val article = TestData.sampleArticleWithByNcSa.copy(copyright=Copyright("beerware", "", Seq()))
    a [ValidationException] should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle does not throw an exception on an article with a valid license") {
    val article = TestData.sampleArticleWithByNcSa.copy(copyright=Copyright("by-sa", "", Seq()))
    noException should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle throws an exception on an article with html in copyright origin") {
    val article = TestData.sampleArticleWithByNcSa.copy(copyright=Copyright("by-sa", "<h1>origin</h1>", Seq()))
    a [ValidationException] should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle does not throw an exception on an article with plain text in copyright origin") {
    val article = TestData.sampleArticleWithByNcSa.copy(copyright=Copyright("by-sa", "", Seq()))
    noException should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle does not throw an exception on an article with plain text in authors field") {
    val article = TestData.sampleArticleWithByNcSa.copy(copyright=Copyright("by-sa", "", Seq(Author("author", "John Doe"))))
    noException should be thrownBy articleValidator.validateArticle(article)
  }

  test("validateArticle throws an exception on an article with html in authors field") {
    val article = TestData.sampleArticleWithByNcSa.copy(copyright=Copyright("by-sa", "", Seq(Author("author", "<h1>john</h1>"))))
    a [ValidationException] should be thrownBy articleValidator.validateArticle(article)
  }

}
