package no.ndla.contentapi.controller

import no.ndla.contentapi.integration.{LanguageContent, NodeToConvert}
import no.ndla.contentapi.model._
import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import org.json4s.native.Serialization._
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.Mockito._
import scala.util.Try

class InternControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats

  val (nodeId, nodeId2) = ("1234", "4321")
  val sampleTitle = ContentTitle("title", Some("en"))
  val sampleContent = LanguageContent(nodeId, nodeId, "content", Some("en"))
  val sampleContent2 = LanguageContent(nodeId, nodeId2, "content", Some("en"))
  val license = License("licence", "description", Some("http://"))
  val author = Author("forfatter", "Henrik")
  val copyright = Copyright(license, "", List(author))
  val sampleNode = NodeToConvert(List(sampleTitle), List(sampleContent), copyright, List(ContentTag(List("tag"), Some("en"))))
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
    when(extractConvertStoreContent.processNode(nodeId)).thenReturn(Try((newNodeid, ImportStatus())))

    post(s"/import/$nodeId") {
      status should equal(200)
      val convertedBody = read[ImportStatus](body)
    }
  }
}
