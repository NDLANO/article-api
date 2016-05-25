package no.ndla.contentapi.batch.service

import no.ndla.contentapi.batch.BatchTestEnvironment
import no.ndla.contentapi.model._
import no.ndla.learningpathapi.UnitSuite

class ConverterServiceTest extends UnitSuite with BatchTestEnvironment {

  val service = new ConverterService

  val contentTitle = ContentTitle("", Some(""))
  val license = License("licence", "description", Some("http://"))
  val author = Author("forfatter", "Henrik")
  val copyright = Copyright(license, "", List(author))
  val tag = ContentTag("asdf", Some("nb"))
  val requiredLibrary = RequiredLibrary("", "", "")

  test("That the document is wrapped in an article tag") {
    val initialContent = "<h1>Heading</h1>"
    val node = ContentInformation("1", List(contentTitle), List(Content(initialContent, Some("nb"))), copyright, List(tag), List(requiredLibrary))
    val expedtedResult = "<article>" + initialContent + "</article>"

    service.convertNode(node).content(0).content.replace("\n", "").replace(" ", "") should equal (expedtedResult)
  }
}
