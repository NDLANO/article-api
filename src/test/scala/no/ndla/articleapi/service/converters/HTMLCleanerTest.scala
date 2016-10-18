package no.ndla.articleapi.service.converters

import no.ndla.articleapi.UnitSuite
import no.ndla.articleapi.integration.LanguageContent
import no.ndla.articleapi.model.ImportStatus

class HTMLCleanerTest extends UnitSuite {
  val nodeId = "1234"

  test("That HTMLCleaner unwraps illegal attributes") {
    val initialContent = LanguageContent(nodeId, nodeId, """<body><article><h1 class="useless">heading<div style="width='0px'">hey</div></h1></article></body>""", Some("en"))
    val expectedResult = "<article><h1>heading<div>hey</div></h1></article>"
    val (result, status) = HTMLCleaner.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That HTMLCleaner unwraps illegal tags") {
    val initialContent = LanguageContent(nodeId, nodeId, """<article><h1>heading</h1><henriktag>hehe</henriktag></article>""", Some("en"))
    val expectedResult = "<article><h1>heading</h1>hehe</article>"
    val (result, status) = HTMLCleaner.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That HTMLCleaner removes comments") {
    val initialContent = LanguageContent(nodeId, nodeId, """<article><!-- this is a comment --><h1>heading<!-- comment --></h1></article>""", Some("en"))
    val expectedResult = "<article><h1>heading</h1></article>"
    val (result, status) = HTMLCleaner.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That isAttributeKeyValid returns false for illegal attributes") {
    HTMLCleaner.isAttributeKeyValid("data-random-junk", "td") should equal (false)
  }

  test("That isAttributeKeyValid returns true for legal attributes") {
    HTMLCleaner.isAttributeKeyValid("align", "td") should equal (true)
  }

  test("That isTagValid returns false for illegal tags") {
    HTMLCleaner.isTagValid("yodawg") should equal (false)
  }

  test("That isTagValid returns true for legal attributes") {
    HTMLCleaner.isTagValid("section") should equal (true)
  }

}
