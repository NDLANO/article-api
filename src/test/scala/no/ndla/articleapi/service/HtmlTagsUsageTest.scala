/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import java.util.Date

import no.ndla.articleapi.model._
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._

class HtmlTagsUsageTest extends UnitSuite with TestEnvironment {
  val embedUrl = "http://hello.yes.this.is.dog"
  val copyright = Copyright(License("publicdomain", "", None), "", List())
  val article1 = Article("1", Seq(ArticleTitle("test", Some("en"))), Seq(ArticleContent("<article><div>test</div></article>", None, Some("en"))), copyright, Seq(), Seq(), Seq(), Seq(), new Date(0), new Date(1), "fagstoff")
  val article2 = Article("2", Seq(ArticleTitle("test", Some("en"))), Seq(ArticleContent("<article><div>test</div><p>paragraph</p></article>", None, Some("en"))), copyright, Seq(), Seq(), Seq(), Seq(), new Date(0), new Date(1), "fagstoff")
  val article3 = Article("3", Seq(ArticleTitle("test", Some("en"))), Seq(ArticleContent("<article><img></img></article>", None, Some("en"))), copyright, Seq(), Seq(), Seq(), Seq(), new Date(0), new Date(1), "fagstoff")
  val article4 = Article("4", Seq(ArticleTitle("test", Some("en"))), Seq(ArticleContent(s"""<article><figure data-resource="external" data-url="$embedUrl""></figure></article>""", None, Some("en"))), copyright, Seq(), Seq(), Seq(), Seq(), new Date(0), new Date(1), "fagstoff")


  test("That getHtmlTagsMap counts html elements correctly") {
    val expectedResult = Map("article" -> List("1", "2", "3"), "div" -> List("1", "2"), "p" -> List("2"), "img" -> List("3"))
    when(articleRepository.all).thenReturn(List(article1, article2, article3))
    ArticleContentInformation.getHtmlTagsMap should equal (expectedResult)
  }

  test("That getHtmlTagsMap returns an empty map if no articles is available") {
    when(articleRepository.all).thenReturn(List())
    ArticleContentInformation.getHtmlTagsMap should equal (Map())
  }

  test("getExternalEmbedResources returns a map with external embed resources") {
    when(articleRepository.all).thenReturn(List(article1, article2, article3, article4))
    ArticleContentInformation.getExternalEmbedResources should equal(Map(article4.id -> Seq(embedUrl)))
  }
}
