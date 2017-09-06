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

class ContentValidatorTest extends UnitSuite with TestEnvironment {
  override val contentValidator = new ContentValidator(allowEmptyLanguageField = false)
  val validDocument = """<section><h1>heisann</h1><h2>heia</h2></section>"""
  val invalidDocument = """<section><invalid></invalid></section>"""

  test("validateArticle does not throw an exception on a valid document") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, "nb")))
    contentValidator.validateArticle(article, false).isSuccess should be (true)
  }

  test("validateArticle throws a validation exception on an invalid document") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(invalidDocument, None, "nb")))
    contentValidator.validateArticle(article, false).isFailure should be (true)
  }

  test("validateArticle does not throw an exception for MathMl tags") {
    val content = """<section><math xmlns="http://www.w3.org/1998/Math/MathML"></math></section>"""
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(content, None, "nb")))

    contentValidator.validateArticle(article, false).isSuccess should be (true)
  }

  test("validateArticle should throw an error if introduction contains HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, "nb")), introduction=Seq(ArticleIntroduction(validDocument, "nb")))
    contentValidator.validateArticle(article, false).isFailure should be (true)
  }

  test("validateArticle should not throw an error if introduction contains plain text") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, "nb")), introduction=Seq(ArticleIntroduction("introduction", "nb")))
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle should throw an error if metaDescription contains HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, "nb")), metaDescription=Seq(ArticleMetaDescription(validDocument, "nb")))
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle should not throw an error if metaDescription contains plain text") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, "nb")), metaDescription=Seq(ArticleMetaDescription("meta description", "nb")))
    contentValidator.validateArticle(article, false).isSuccess should be (true)
  }

  test("validateArticle should throw an error if title contains HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, "nb")), title=Seq(ArticleTitle(validDocument, "nb")))
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle should not throw an error if title contains plain text") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, "nb")), title=Seq(ArticleTitle("title", "nb")))
    contentValidator.validateArticle(article, false).isSuccess should be (true)
  }

  test("Validation should fail if content contains other tags than section on root") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent("<h1>lolol</h1>", None, "nb")))
    val result = contentValidator.validateArticle(article, false)
    result.isFailure should be (true)

    val validationMessage = result.failed.get.asInstanceOf[ValidationException].errors.head.message
    validationMessage.contains("An article must consist of one or more <section> blocks") should be (true)
  }

  test("validateArticle throws a validation exception on an invalid visual element") {
    val invalidVisualElement = TestData.visualElement.copy(resource=invalidDocument)
    val article = TestData.sampleArticleWithByNcSa.copy(visualElement=Seq(invalidVisualElement))
    contentValidator.validateArticle(article, false).isFailure should be (true)
  }

  test("validateArticle does not throw an exception on a valid visual element") {
    val article = TestData.sampleArticleWithByNcSa.copy(visualElement=Seq(TestData.visualElement))
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle does not throw an exception on an article with plaintext tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(tags=Seq(ArticleTag(Seq("vann", "snø", "sol"), "nb")))
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with html in tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(tags=Seq(ArticleTag(Seq("<h1>vann</h1>", "snø", "sol"), "nb")))
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article where metaImageId is a number") {
    val article = TestData.sampleArticleWithByNcSa.copy(metaImageId=Some("123"))
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article where metaImageId is not a number") {
    val article = TestData.sampleArticleWithByNcSa.copy(metaImageId=Some("not a number"))
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle throws an exception on an article with an illegal required library") {
    val illegalRequiredLib = RequiredLibrary("text/javascript", "naughty", "http://scary.bad.source.net/notNice.js")
    val article = TestData.sampleArticleWithByNcSa.copy(requiredLibraries=Seq(illegalRequiredLib))
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with a legal required library") {
    val illegalRequiredLib = RequiredLibrary("text/javascript", "h5p", H5PResizerScriptUrl)
    val article = TestData.sampleArticleWithByNcSa.copy(requiredLibraries=Seq(illegalRequiredLib))
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with an invalid license") {
    val article = TestData.sampleArticleWithByNcSa.copy(copyright=Copyright("beerware", "", Seq()))
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with a valid license") {
    val article = TestData.sampleArticleWithByNcSa.copy(copyright=Copyright("by-sa", "", Seq()))
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with html in copyright origin") {
    val article = TestData.sampleArticleWithByNcSa.copy(copyright=Copyright("by-sa", "<h1>origin</h1>", Seq()))
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with plain text in copyright origin") {
    val article = TestData.sampleArticleWithByNcSa.copy(copyright=Copyright("by-sa", "", Seq()))
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle does not throw an exception on an article with plain text in authors field") {
    val article = TestData.sampleArticleWithByNcSa.copy(copyright=Copyright("by-sa", "", Seq(Author("author", "John Doe"))))
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with html in authors field") {
    val article = TestData.sampleArticleWithByNcSa.copy(copyright=Copyright("by-sa", "", Seq(Author("author", "<h1>john</h1>"))))
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle throws an exception on an article with an invalid article type") {
    val article = TestData.sampleArticleWithByNcSa.copy(articleType = "invalid")
    contentValidator.validateArticle(article, false).isFailure should be (true)
  }

  test("Validation should fail if concept content contains html") {
    val concept = TestData.sampleConcept.copy(content=Seq(ConceptContent("<h1>lolol</h1>", "nb")))
    contentValidator.validate(concept).isFailure should be (true)
  }

  test("Validation should fail if concept title contains html") {
    val concept = TestData.sampleConcept.copy(title=Seq(ConceptTitle("<h1>lolol</h1>", "nb")))
    contentValidator.validate(concept).isFailure should be (true)
  }

  test("Validation should succeed if concept contains no html") {
    val concept = TestData.sampleConcept.copy(title=Seq(ConceptTitle("lolol", "nb")), content=Seq(ConceptContent("lolol", "nb")))
    contentValidator.validate(concept).isSuccess should be (true)
  }

  test("Validation should not fail with language=unknown if allowUnknownLanguage is set") {
    val article = TestData.sampleArticleWithByNcSa.copy(title = Seq(ArticleTitle("tittele", "unknown")))
    contentValidator.validateArticle(article, true).isSuccess should be (true)
  }

}
