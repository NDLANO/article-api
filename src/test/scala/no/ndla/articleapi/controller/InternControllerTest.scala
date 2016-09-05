/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.controller

import no.ndla.articleapi.integration.{LanguageContent, MigrationRelatedContent, MigrationRelatedContents}
import no.ndla.articleapi.model._
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import org.json4s.native.Serialization._
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.Mockito._

import scala.util.{Failure, Try}

class InternControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats

  val (nodeId, nodeId2) = ("1234", "4321")
  val sampleTitle = ArticleTitle("title", Some("en"))
  val sampleContent = LanguageContent(nodeId, nodeId, "content", Some("en"), None)
  val sampleContent2 = LanguageContent(nodeId, nodeId2, "content", Some("en"), None)
  val license = License("licence", "description", Some("http://"))
  val author = Author("forfatter", "Henrik")
  val copyright = Copyright(license, "", List(author))
  val pageTitle = PageTitle("Fanetittel", "type", Some("nb"))
  val visualElement = VisualElement("http://image-api/1", "image", Some("nb"))
  val relatedContents = MigrationRelatedContents(Seq(MigrationRelatedContent("1234", "Programmering", ".../#fordypning", 1)), Some("nb"))
  val sampleNode = NodeToConvert(List(sampleTitle), List(sampleContent), copyright, List(ArticleTag(List("tag"), Some("en"))), Seq(pageTitle), Seq(visualElement), Seq(relatedContents))
  val sampleNode2 = sampleNode.copy(contents=List(sampleContent2))

  lazy val controller = new InternController
  addServlet(controller, "/*")

  test("That POST /import/:node_id returns 500 if the main node is not found") {

    when(extractService.getNodeData(nodeId2)).thenReturn(sampleNode2)

    post(s"/import/$nodeId2") {
      status should equal(500)
    }
  }

  test("That POST /import/:node_id returns a json status-object on success") {
    val newNodeid: Long = 4444
    when(extractConvertStoreContent.processNode(nodeId)).thenReturn(Try((newNodeid, ImportStatus(Seq(), Seq()))))

    post(s"/import/$nodeId") {
      status should equal(200)
      val convertedBody = read[ImportStatus](body)
      convertedBody should equal (ImportStatus(s"Successfully imported node $nodeId: $newNodeid", Seq()))
    }
  }

  test("That POST /import/:node_id status code is 500 with a message if processNode fails") {
    when(extractConvertStoreContent.processNode(nodeId)).thenReturn(Failure(new RuntimeException("processNode failed")))

    post(s"/import/$nodeId") {
      status should equal(500)
    }
  }

}
