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
import no.ndla.articleapi.service.converters.{Attributes, ResourceType}
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._

class ReadServiceTest extends UnitSuite with TestEnvironment {
  override val readService = new ReadService

  val externalImageApiUrl = externalApiUrls("image")
  val idAttr = s"${Attributes.DataId}"
  val resourceIdAttr = s"${Attributes.DataResource_Id}"
  val resourceAttr = s"${Attributes.DataResource}"
  val imageType = s"${ResourceType.Image}"
  val h5pType = s"${ResourceType.H5P}"
  val urlAttr = s"${Attributes.DataUrl}"
  val content1 = s"""<$resourceHtmlEmbedTag $resourceIdAttr=123 $resourceAttr="$imageType" /><$resourceHtmlEmbedTag $resourceIdAttr=1234 $resourceAttr="$imageType" />"""
  val content2 = s"""<$resourceHtmlEmbedTag $resourceIdAttr=321 $resourceAttr="$imageType" /><$resourceHtmlEmbedTag $resourceIdAttr=4321 $resourceAttr="$imageType" />"""
  val articleContent1 = ArticleContent(content1, None, None)
  val expectedArticleContent1 = articleContent1.copy(content=
    s"""<$resourceHtmlEmbedTag $resourceAttr="$imageType" $idAttr="0" $urlAttr="$externalImageApiUrl/123" /><$resourceHtmlEmbedTag $resourceAttr="$imageType" $idAttr="1" $urlAttr="$externalImageApiUrl/1234" />""")

  val articleContent2 = ArticleContent(content2, None, None)

  test("withId adds urls and ids on embed resources") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(articleContent1))
    val expectedResult = converterService.toApiArticle(article.copy(content=Seq(expectedArticleContent1)))
    when(articleRepository.withId(1)).thenReturn(Option(article))

    readService.withId(1) should equal(Option(expectedResult))
  }

  test("addIdAndUrlOnResource adds an id and url attribute on embed-resoures with a data-resource_id attribute") {
    readService.addIdAndUrlOnResource(articleContent1) should equal(expectedArticleContent1)
  }

  test("addIdAndUrlOnResource adds id but not url on embed resources without a data-resource_id attribute") {
    val articleContent3 = articleContent1.copy(content=s"""<$resourceHtmlEmbedTag $resourceAttr="$h5pType" $idAttr="0" $urlAttr="http://some.h5p.org" />""")
    readService.addIdAndUrlOnResource(articleContent3) should equal(articleContent3)
  }

  test("addIdAndUrlOnResource adds urls on all content translations in an article") {
    val article = TestData.sampleArticleWithByNcSa.copy(content=Seq(articleContent1, articleContent2))
    val article1ExpectedResult = articleContent1.copy(content=
      s"""<$resourceHtmlEmbedTag $resourceAttr="$imageType" $idAttr="0" $urlAttr="$externalImageApiUrl/123" /><$resourceHtmlEmbedTag $resourceAttr="$imageType" $idAttr="1" $urlAttr="$externalImageApiUrl/1234" />""")
    val article2ExpectedResult = articleContent1.copy(content=
      s"""<$resourceHtmlEmbedTag $resourceAttr="$imageType" $idAttr="0" $urlAttr="$externalImageApiUrl/321" /><$resourceHtmlEmbedTag $resourceAttr="$imageType" $idAttr="1" $urlAttr="$externalImageApiUrl/4321" />""")

    val result = readService.addUrlsAndIdsOnEmbedResources(article)
    result should equal (article.copy(content=Seq(article1ExpectedResult, article2ExpectedResult)))
  }
}