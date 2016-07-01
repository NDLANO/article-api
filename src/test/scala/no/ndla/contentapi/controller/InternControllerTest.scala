package no.ndla.contentapi.controller

import no.ndla.contentapi.ComponentRegistry._
import no.ndla.contentapi.integration.{LanguageContent, NodeToConvert}
import no.ndla.contentapi.model._
import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import org.json4s.native.Serialization._
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.Mockito._

class InternControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats

  val (nodeId, nodeId2) = ("1234", "4321")
  val sampleTitle = ContentTitle("title", Some("en"))
  val sampleContent = LanguageContent(nodeId, nodeId, "content", Some("en"))
  val sampleContent2 = LanguageContent(nodeId, nodeId2, "content", Some("en"))
  val license = License("licence", "description", Some("http://"))
  val author = Author("forfatter", "Henrik")
  val copyright = Copyright(license, "", List(author))
  val sampleNode = NodeToConvert(List(sampleTitle), List(sampleContent), copyright, List(ContentTag("tag", Some("en"))))
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
    val newNodeid = 4444
    when(extractService.getNodeData(nodeId)).thenReturn(sampleNode)
    when(converterService.convertNode(sampleNode)).thenReturn((sampleNode.asContentInformation, ImportStatus()))
    when(contentRepository.exists(sampleNode.contents.head.nid)).thenReturn(true)
    when(contentRepository.update(sampleNode.asContentInformation, nodeId)).thenReturn(newNodeid)

    post(s"/import/$nodeId") {
      status should equal(200)
      val convertedBody = read[ImportStatus](body)
      convertedBody.messages.head should equal (s"Successfully imported nodes $nodeId: $newNodeid")
    }
  }
}
