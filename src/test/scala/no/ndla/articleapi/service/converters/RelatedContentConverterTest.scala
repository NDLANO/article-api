/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.service.converters

import no.ndla.articleapi.integration.MigrationRelatedContent
import no.ndla.articleapi.model.domain.ImportStatus
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import Attributes._
import ResourceType._
import no.ndla.articleapi.model.api.ImportException
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.util.{Failure, Success}

class RelatedContentConverterTest extends UnitSuite with TestEnvironment {
  val relatedContent1 = MigrationRelatedContent("1234", "Tittel", "uri", 1)
  val relatedContent2 = MigrationRelatedContent("5678", "Palma", "uri", 1)
  val languageContent = TestData.sampleContent.copy(relatedContent = Seq(relatedContent1, relatedContent2))

  override def beforeEach() {
    when(extractService.getNodeType("1234")).thenReturn(Some("fagstoff"))
    when(extractService.getNodeType("5678")).thenReturn(Some("fagstoff"))
  }

  test("convert should insert a new section with an related-content embed tag") {
    val origContent = "<section><h1>hmm</h1></section>"

    when(extractConvertStoreContent
      .processNode(any[String], any[ImportStatus], any[Boolean])).thenReturn(Success((TestData.sampleArticleWithByNcSa.copy(id=Some(1)), ImportStatus.empty)))
      .thenReturn(Success((TestData.sampleArticleWithByNcSa.copy(id=Some(2)), ImportStatus.empty)))

    val expectedContent = origContent + s"""<section><$resourceHtmlEmbedTag $DataArticleIds="1,2" $DataResource="$RelatedContent"></section>"""

    val Success((result, _)) = RelatedContentConverter.convert(languageContent.copy(content=origContent), ImportStatus.empty)
    result.content should equal (expectedContent)
  }

  test("convert should return a Failure if trying to link to a concept as related content") {
    when(extractConvertStoreContent.processNode(any[String], any[ImportStatus], any[Boolean]))
      .thenReturn(Success((TestData.sampleConcept.copy(id=Some(1)), ImportStatus.empty)))
      .thenReturn(Success((TestData.sampleArticleWithByNcSa.copy(id=Some(2)), ImportStatus.empty)))

    val result = RelatedContentConverter.convert(languageContent, ImportStatus.empty)
    result should equal (Failure(ImportException("Failed to import one or more related contents: Related content points to a concept. This should not be legal, no?")))
  }

  test("convert should not add a new section if thre are no related contents") {
    val origContent = "<section><h1>hmm</h1></section>"

    val Success((result, _)) = RelatedContentConverter.convert(languageContent.copy(content=origContent, relatedContent=Seq.empty), ImportStatus.empty)
    result.content should equal (origContent)
  }

  test("convert should not add related nodes of unsupported types") {
    val origContent = "<section><h1>hmm</h1></section>"

    when(extractService.getNodeType("5678")).thenReturn(Some("link"))
    when(extractConvertStoreContent.processNode("1234", ImportStatus.empty)).thenReturn(Success((TestData.sampleArticleWithByNcSa.copy(id=Some(1)), ImportStatus.empty)))

    val expectedContent = origContent + s"""<section><$resourceHtmlEmbedTag $DataArticleIds="1" $DataResource="$RelatedContent"></section>"""

    val Success((result, _)) = RelatedContentConverter.convert(languageContent.copy(content=origContent), ImportStatus.empty)
    result.content should equal (expectedContent)
  }
}
