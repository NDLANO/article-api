/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import org.mockito.Mockito._
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.TestData

class HtmlTagsUsageTest extends UnitSuite with TestEnvironment {
  val embedUrl = "http://hello.yes.this.is.dog"
  val copyright = Copyright("publicdomain", "", List())

  val article1 = TestData.sampleArticleWithPublicDomain.copy(id=Option(1), content=Seq(ArticleContent("<section><div>test</div></section>", None, "en")))
  val article2 = TestData.sampleArticleWithPublicDomain.copy(id=Option(2), content=Seq(ArticleContent("<article><div>test</div><p>paragraph</p></article>", None, "en")))
  val article3 = TestData.sampleArticleWithPublicDomain.copy(id=Option(3), content=Seq(ArticleContent("<article><img></img></article>", None, "en")))
  val article4 = TestData.sampleArticleWithPublicDomain.copy(id=Option(4), content=Seq(ArticleContent(s"""<article><$resourceHtmlEmbedTag data-resource="external" data-url="$embedUrl"" /></article>""", None, "en")))


  test("That getHtmlTagsMap counts html elements correctly") {
    val expectedResult = Map("section" -> List(1), "article" -> List(2, 3), "div" -> List(1, 2), "p" -> List(2), "img" -> List(3))
    when(articleRepository.all).thenReturn(List(article1, article2, article3))
    ArticleContentInformation.getHtmlTagsMap should equal (expectedResult)
  }

  test("That getHtmlTagsMap returns an empty map if no articles is available") {
    when(articleRepository.all).thenReturn(List())
    ArticleContentInformation.getHtmlTagsMap should equal (Map())
  }

  test("getExternalEmbedResources returns a map with external embed resources") {
    val (externalId, externalSubjectId) = ("1234", "52")
    when(articleRepository.allWithExternalSubjectId(externalSubjectId)).thenReturn(List(article4))
    when(articleRepository.getExternalIdFromId(article4.id.get)).thenReturn(Some(externalId))
    ArticleContentInformation.getExternalEmbedResources(externalSubjectId) should equal(Map(externalId -> Seq(embedUrl)))
  }
}
