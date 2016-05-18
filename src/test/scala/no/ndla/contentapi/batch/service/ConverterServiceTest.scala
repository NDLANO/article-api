package no.ndla.contentapi.batch.service

import no.ndla.contentapi.batch.BatchTestEnvironment
import no.ndla.learningpathapi.UnitSuite

class ConverterServiceTest extends UnitSuite with BatchTestEnvironment {

  val service = new ConverterService

  test("That the document is wrapped in an article tag") {
    val initialContent = "<h1>Heading</h1>"
    val expedtedResult = "<article>" + initialContent + "</article>"

    service.convert(initialContent).replace("\n", "").replace(" ", "") should equal (expedtedResult)
  }
}
