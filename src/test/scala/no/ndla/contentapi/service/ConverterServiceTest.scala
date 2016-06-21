package no.ndla.contentapi.service

import no.ndla.contentapi.TestEnvironment
import no.ndla.contentapi.model._
import no.ndla.contentapi.UnitSuite

class ConverterServiceTest extends UnitSuite with TestEnvironment {

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

    service.convertNode(node)._1.content(0).content.replace("\n", "").replace(" ", "") should equal (expedtedResult)
  }
}
