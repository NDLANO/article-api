/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.articleapi.model.api.ValidationException
import no.ndla.articleapi.model.domain.ArticleContent

class ValidationServiceTest extends UnitSuite with TestEnvironment {
  override val validationService = new ValidationService
  val validDocument =
    """<h1>heisann</h1>
      |<h2>heia</h2>""".stripMargin

  val invalidDocument =
    """<article>
      |<h1>heisann</h1>
      |</article>
      |""".stripMargin

  test("validateArticleContent does not throw an exception on a valid schema 2") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(validDocument, None, Some("nb"))))
    noException should be thrownBy validationService.validateArticle(article)
  }

  test("validateArticleContent throws a validation exception on an invalid schema") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(ArticleContent(invalidDocument, None, Some("nb"))))
    a [ValidationException] should be thrownBy validationService.validateArticle(article)
  }
}
