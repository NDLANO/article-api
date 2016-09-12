/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import no.ndla.articleapi.model._
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._

class HtmlTagsUsageTest extends UnitSuite with TestEnvironment {

  val copyright = Copyright(License("publicdomain", "", None), "", List())
  val article1 = ArticleInformation("1", Seq(ArticleTitle("test", Some("en"))), Seq(Article("<article><div>test</div></article>", None, Some("en"))), copyright, Seq(), Seq(), Seq(), Seq(), Seq(), Seq(), 0, 1)
  val article2 = ArticleInformation("2", Seq(ArticleTitle("test", Some("en"))), Seq(Article("<article><div>test</div><p>paragraph</p></article>", None, Some("en"))), copyright, Seq(), Seq(), Seq(), Seq(), Seq(), Seq(), 0, 1)
  val article3 = ArticleInformation("3", Seq(ArticleTitle("test", Some("en"))), Seq(Article("<article><img></img></article>", None, Some("en"))), copyright, Seq(), Seq(), Seq(), Seq(), Seq(), Seq(), 0, 1)

  test("That getHtmlTagsMap counts html elements correctly") {
    val expectedResult = Map("article" -> List("1", "2", "3"), "div" -> List("1", "2"), "p" -> List("2"), "img" -> List("3"))
    when(articleRepository.all).thenReturn(List(article1, article2, article3))
    HtmlTagsUsage.getHtmlTagsMap should equal (expectedResult)
  }

  test("That getHtmlTagsMap returns an empty map if no articles is available") {
    when(articleRepository.all).thenReturn(List())
    HtmlTagsUsage.getHtmlTagsMap should equal (Map())
  }
}
