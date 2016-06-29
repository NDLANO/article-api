package no.ndla.contentapi.controller

import no.ndla.contentapi.integration.{LanguageContent, NodeToConvert}
import no.ndla.contentapi.model._
import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.Mockito._

class InternControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  val (nodeId, nodeId2) = ("1234", "4321")
  val sampleTitle = ContentTitle("title", Some("en"))
  val sampleContent = LanguageContent(nodeId, nodeId2, "content", Some("en"))
  val license = License("licence", "description", Some("http://"))
  val author = Author("forfatter", "Henrik")
  val copyright = Copyright(license, "", List(author))
  val sampleNode = NodeToConvert(List(sampleTitle), List(sampleContent), copyright, List(ContentTag("tag", Some("en"))))

  lazy val controller = new InternController
  addServlet(controller, "/*")

  test("That POST /import/:node_id returns 500 if the main node is not found") {

    when(extractService.importNode(nodeId)).thenReturn(sampleNode)

    post(s"/import/$nodeId") {
      status should equal(500)
    }
  }
}
