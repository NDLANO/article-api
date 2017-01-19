/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.ArticleApiProperties.{externalApiUrls, resourceHtmlEmbedTag}
import no.ndla.articleapi.model.domain.ArticleContent
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._

class ReadServiceTest extends UnitSuite with TestEnvironment {
  override val readService = new ReadService

  val externalImageApiUrl = externalApiUrls("image")
  val content1 = s"""<$resourceHtmlEmbedTag data-resource_id=123 data-resource="image" /><$resourceHtmlEmbedTag data-resource_id=1234 data-resource="image" />"""
  val content2 = s"""<$resourceHtmlEmbedTag data-resource_id=321 data-resource="image" /><$resourceHtmlEmbedTag data-resource_id=4321 data-resource="image" />"""
  val articleContent1 = ArticleContent(content1, None, None)
  val expectedArticleContent1 = articleContent1.copy(content=
    s"""<$resourceHtmlEmbedTag data-resource="image" data-url="$externalImageApiUrl/123" /><$resourceHtmlEmbedTag data-resource="image" data-url="$externalImageApiUrl/1234" />""")

  val articleContent2 = ArticleContent(content2, None, None)

  test("withId adds urls on embed resources") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(articleContent1))
    val expectedResult = converterService.toApiArticle(article.copy(content=Seq(expectedArticleContent1)))
    when(articleRepository.withId(1)).thenReturn(Option(article))

    readService.withId(1) should equal(Option(expectedResult))
  }

  test("addUrlOnResource adds an url attribute on embed-resoures with a data-resourceId attribute") {
    readService.addUrlOnResource(articleContent1) should equal(expectedArticleContent1)
  }

  test("addUrlOnResource does not add url on embed resources without a data-resource_id attribute") {
    val articleContent3 = articleContent1.copy(content=s"""<$resourceHtmlEmbedTag data-resource="h5p" url="http://some.h5p.org" />""")
    readService.addUrlOnResource(articleContent3) should equal(articleContent3)
  }

  test("addUrlsOnEmbedResources adds urls on all content translations in an article") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(articleContent1, articleContent2))
    val article1ExpectedResult = articleContent1.copy(content=
      s"""<$resourceHtmlEmbedTag data-resource="image" data-url="$externalImageApiUrl/123" /><$resourceHtmlEmbedTag data-resource="image" data-url="$externalImageApiUrl/1234" />""")
    val article2ExpectedResult = articleContent1.copy(content=
      s"""<$resourceHtmlEmbedTag data-resource="image" data-url="$externalImageApiUrl/321" /><$resourceHtmlEmbedTag data-resource="image" data-url="$externalImageApiUrl/4321" />""")

    val result = readService.addUrlsOnEmbedResources(article)
    result should equal (article.copy(content=Seq(article1ExpectedResult, article2ExpectedResult)))
  }
}
