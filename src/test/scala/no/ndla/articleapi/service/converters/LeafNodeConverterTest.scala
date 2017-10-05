/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */


package no.ndla.articleapi.service.converters

import no.ndla.articleapi.model.domain.ImportStatus
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.integration.MigrationEmbedMeta
import no.ndla.articleapi.model.api.ImportException
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.util.{Failure, Success}

class LeafNodeConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"

  test("Leaf node converter should create an article from a pure video node") {
    val sampleLanguageContent = TestData.sampleContent.copy(content="", nodeType = "video")
    val expectedResult = s"""<section><$resourceHtmlEmbedTag data-account="some-account-id" data-caption="" data-player="some-player-id" data-resource="brightcove" data-videoid="ref:${sampleLanguageContent.nid}"></section>"""
    val Success((result, _)) = LeafNodeConverter.convert(sampleLanguageContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.size should equal (1)
  }

  test("Leaf node converter should create an article from a pure h5p node") {
    val sampleLanguageContent = TestData.sampleContent.copy(content="<div><h1>hi</h1></div>", nodeType = "h5p_content")
    val expectedResult = s"""${sampleLanguageContent.content}<section><$resourceHtmlEmbedTag data-resource="h5p" data-url="//ndla.no/h5p/embed/1234"></section>"""
    val Success((result, _)) = LeafNodeConverter.convert(sampleLanguageContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.size should equal (1)
  }

  test("Leaf node converter should create an article from a pure lenke/ekstern ressurs node") {
    val sampleLanguageContent = TestData.sampleContent.copy(content="<div><h1>hi</h1></div>", nodeType = "lenke", contentType = Some("ekstern ressurs"))
    val url = "https://www.youtube.com/watch?v=dNfpRZi42hQ"

    when(extractService.getNodeEmbedMeta(any[String])).thenReturn(Success(MigrationEmbedMeta(Some(url), Some("<iframe src=\"$url\""))))

    val expectedResult = s"""${sampleLanguageContent.content}<section><$resourceHtmlEmbedTag data-resource="external" data-url="$url"></section>"""
    val Success((result, _)) = LeafNodeConverter.convert(sampleLanguageContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.size should equal (0)
  }

  test("Leaf node converter should return a failure if node type is unsupported") {
    val sampleLanguageContent = TestData.sampleContent.copy(nodeType = "lolol")

    val Failure(ex) = LeafNodeConverter.convert(sampleLanguageContent, ImportStatus.empty)
    ex.isInstanceOf[ImportException] should be (true)
    ex.getMessage.contains("unsupported node/content -type:") should be(true)
  }

}
