/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import java.util.Date

import io.searchbox.client.JestResult
import no.ndla.articleapi.integration.{LanguageIngress, MigrationSubjectMeta}
import no.ndla.articleapi.model.api.{OptimisticLockException, ValidationError, ValidationException, ValidationMessage}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Matchers._
import org.mockito.Mockito._
import scalikejdbc.DBSession

import scala.util.{Failure, Success, Try}

class ExtractConvertStoreContentTest extends UnitSuite with TestEnvironment {
  override val converterService = new ConverterService
  val (nodeId, nodeId2) = ("1234", "4321")
  val newNodeid: Long = 4444
  val sampleTitle = ArticleTitle("title", "en")
  val sampleIngress =  LanguageIngress("ingress here", None)
  val contentString = s"[contentbrowser ==nid=$nodeId2==imagecache=Fullbredde==width===alt=alttext==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=link==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
  val sampleContent = TestData.sampleContent.copy(content=contentString)
  val author = Author("forfatter", "Henrik")

  val sampleNode = NodeToConvert(List(sampleTitle), List(sampleContent), "by-sa", Seq(author), List(ArticleTag(List("tag"), "en")), "fagstoff", "fagstoff", new Date(0), new Date(1), ArticleType.Standard)

  val eCSService = new ExtractConvertStoreContent

  override def beforeEach = {
    when(extractService.getNodeData(nodeId)).thenReturn(sampleNode)
    when(extractService.getNodeType(nodeId2)).thenReturn(Some("fagstoff"))
    when(extractService.getNodeGeneralContent(nodeId2)).thenReturn(Seq(NodeGeneralContent(nodeId2, nodeId2, "title", "content", "en")))
    when(articleRepository.getIdFromExternalId(nodeId2)).thenReturn(None)
    when(migrationApiClient.getSubjectForNode(nodeId)).thenReturn(Try(Seq(MigrationSubjectMeta("52", "helsearbeider vg2"))))

    when(readService.getContentByExternalId(any[String])).thenReturn(None)
    when(importValidator.validate(any[Article], any[Boolean])).thenReturn(Success(TestData.sampleArticleWithByNcSa))
    when(articleRepository.exists(sampleNode.contents.head.nid)).thenReturn(false)
    when(articleRepository.insertWithExternalIds(any[Article], any[String], any[Seq[String]])(any[DBSession])).thenReturn(TestData.sampleArticleWithPublicDomain)
    when(extractConvertStoreContent.processNode("9876")).thenReturn(Try(TestData.sampleArticleWithPublicDomain, ImportStatus(Seq(), Seq())))
    when(articleIndexService.indexDocument(any[Article])).thenReturn(Success(mock[Article]))
  }

  test("That ETL extracts, translates and loads a node correctly") {
    val sampleArticle = TestData.sampleArticleWithPublicDomain
    when(extractConvertStoreContent.processNode(nodeId2, ImportStatus(Seq(), Seq(nodeId)))).thenReturn(Try((sampleArticle, ImportStatus(Seq(), Seq(nodeId, nodeId2)))))

    val result = eCSService.processNode(nodeId)
    result should equal(Success(sampleArticle, ImportStatus(List(s"Successfully imported node $nodeId: 1"), List(nodeId, nodeId2), sampleArticle.id)))
    verify(articleRepository, times(1)).insertWithExternalIds(any[Article], any[String], any[Seq[String]])
  }

  test("That ETL returns a list of visited nodes") {
    val sampleArticle = TestData.sampleArticleWithPublicDomain
    when(extractConvertStoreContent.processNode(nodeId2, ImportStatus(Seq(), Seq("9876", nodeId)))).thenReturn(Try((sampleArticle, ImportStatus(Seq(), Seq("9876", nodeId, nodeId2)))))

    val result = eCSService.processNode(nodeId, ImportStatus(Seq(), Seq("9876")))
    result should equal(Success(sampleArticle, ImportStatus(List(s"Successfully imported node $nodeId: 1"), List("9876", nodeId, nodeId2), sampleArticle.id)))
  }

  test("That ETL returns a Failure if the node was not found") {
    when(extractService.getNodeData(nodeId)).thenReturn(sampleNode.copy(contents=Seq()))
    when(articleRepository.getIdFromExternalId(nodeId)).thenReturn(None)

    val result = eCSService.processNode(nodeId, ImportStatus(Seq(), Seq("9876")))
    result.isFailure should be (true)
  }

  test("ETL should return a Failure if validation fails") {
    val validationMessage = ValidationMessage("content.content", "Content can not be empty")
    when(importValidator.validate(any[Article], any[Boolean])).thenReturn(Failure(new ValidationException(errors=Seq(validationMessage))))
    when(articleRepository.getIdFromExternalId(nodeId)).thenReturn(Some(1: Long))
    when(articleRepository.getIdFromExternalId(nodeId2)).thenReturn(Some(2: Long))

    val result = eCSService.processNode(nodeId, ImportStatus(Seq(), Seq()))

    result.isFailure should be (true)
    result.failed.get.isInstanceOf[ValidationException] should be (true)
    verify(articleRepository, times(1)).delete(1: Long)
    verify(articleRepository, times(0)).delete(2: Long)
    verify(articleIndexService, times(1)).deleteDocument(1)
    verify(articleIndexService, times(0)).deleteDocument(2)
  }

  test("That ETL returns a Failure if failed to persist the converted article") {
    when(articleRepository.updateWithExternalId(any[Article], any[String])).thenReturn(Failure(new OptimisticLockException()))
    when(articleRepository.exists(sampleNode.contents.head.nid)).thenReturn(true)
    when(articleRepository.getIdFromExternalId(nodeId)).thenReturn(None)

    val result = eCSService.processNode(nodeId, ImportStatus(Seq(), Seq("9876")))
    result.isFailure should be (true)
  }

  test("That ETL returns a Failure if failed to index the converted article") {
    when(articleIndexService.indexDocument(any[Article])).thenReturn(Failure(mock[NdlaSearchException]))
    when(articleRepository.getIdFromExternalId(nodeId)).thenReturn(None)

    val result = eCSService.processNode(nodeId, ImportStatus(Seq(), Seq("9876")))
    result.isFailure should be (true)
  }

  test("Articles that fails to import should be deleted from database if it exists") {
    reset(articleRepository, articleIndexService)
    when(articleIndexService.indexDocument(any[Article])).thenReturn(Failure(mock[RuntimeException]))
    when(articleRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(Some(1: Long))

    val result = eCSService.processNode(nodeId, ImportStatus.empty)
    result.isFailure should be (true)

    verify(articleRepository, times(1)).delete(1)
    verify(articleIndexService, times(1)).deleteDocument(1)
  }

  test("Articles should be force-updated if flag is set") {
    val status = ImportStatus.empty.setForceUpdateArticle(true)
    val sampleArticle = TestData.sampleArticleWithPublicDomain
    when(extractConvertStoreContent.processNode(nodeId2, status.addVisitedNode(nodeId))).thenReturn(Try((sampleArticle, ImportStatus(Seq(), Seq(nodeId, nodeId2)))))
    when(articleRepository.exists(nodeId)).thenReturn(true)
    when(articleRepository.updateWithExternalIdOverrideManualChanges(any[Article], any[String])(any[DBSession])).thenReturn(Success(sampleArticle))

    val result = eCSService.processNode(nodeId, status)
    result should equal(Success(sampleArticle, ImportStatus(List(s"Successfully imported node $nodeId: 1"), List(nodeId, nodeId2), sampleArticle.id)))
    verify(articleRepository, times(1)).updateWithExternalIdOverrideManualChanges(any[Article], any[String])
    verify(articleRepository, times(0)).updateWithExternalId(any[Article], any[String])
  }

  test("Articles should not be force-updated if flag is not set") {
    val status = ImportStatus.empty
    val sampleArticle = TestData.sampleArticleWithPublicDomain
    reset(articleRepository)
    when(extractConvertStoreContent.processNode(nodeId2, status.addVisitedNode(nodeId))).thenReturn(Try((sampleArticle, ImportStatus(Seq(), Seq(nodeId, nodeId2)))))
    when(articleRepository.exists(nodeId)).thenReturn(true)
    when(articleRepository.updateWithExternalId(any[Article], any[String])(any[DBSession])).thenReturn(Success(sampleArticle))

    val result = eCSService.processNode(nodeId, status)
    result should equal(Success(sampleArticle, ImportStatus(List(s"Successfully imported node $nodeId: 1"), List(nodeId, nodeId2), sampleArticle.id)))
    verify(articleRepository, times(0)).updateWithExternalIdOverrideManualChanges(any[Article], any[String])
    verify(articleRepository, times(1)).updateWithExternalId(any[Article], any[String])
  }

}
