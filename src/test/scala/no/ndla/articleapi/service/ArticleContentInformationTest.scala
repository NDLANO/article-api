/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import org.mockito.Mockito._
import no.ndla.articleapi.model.domain.ArticleContent
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag

class ArticleContentInformationTest extends UnitSuite with TestEnvironment {
  val articleId: Long = 1

  test("getEmbedImageWithParentHtml returns a list of images in an article") {
    val content = ArticleContent(s"""<section><p><$resourceHtmlEmbedTag data-resource="image" /></p><div><$resourceHtmlEmbedTag data-resource="image" /></div></section>""", None, None)
    val expectedResult = Seq(s"""<p><$resourceHtmlEmbedTag data-resource="image" /></p>""", s"""<div><$resourceHtmlEmbedTag data-resource="image" /></div>""")

    when(articleRepository.withId(articleId)).thenReturn(Option(TestData.sampleArticleWithByNcSa.copy(content=Seq(content))))
    ArticleContentInformation.getEmbedImageWithParentHtml(articleId).get should equal(expectedResult)
  }

  test("getEmbedImageWithParentHtml returns an empty list if no images were found") {
    val content = ArticleContent("<h1>heading</h1>", None, None)

    when(articleRepository.withId(articleId)).thenReturn(Option(TestData.sampleArticleWithByNcSa.copy(content=Seq(content))))
    ArticleContentInformation.getEmbedImageWithParentHtml(articleId).get.size should equal(0)
  }

  test("getEmbedImageWithParentHtml returns None if the article does not exist") {
    when(articleRepository.withId(articleId)).thenReturn(None)
    ArticleContentInformation.getEmbedImageWithParentHtml(articleId) should equal(None)
  }
}
