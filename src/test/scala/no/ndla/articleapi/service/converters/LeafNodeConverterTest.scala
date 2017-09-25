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

import scala.util.Success

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

}
