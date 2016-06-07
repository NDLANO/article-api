package no.ndla.contentapi.service.converters

import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.learningpathapi.UnitSuite
import org.jsoup.Jsoup

import scala.collection.mutable.ListBuffer

class SimpleTagConverterTest extends UnitSuite {
  test("That divs with class 'paragraph' is removed") {
    val initialContent = "<article><h1>heading</h1><div class='paragraph'>I know words, I have the best words.</div></article>"
    val expectedResult = "<article> <h1>heading</h1>I know words, I have the best words.</article>"
    val (element, requiredLibraries, errors) = SimpleTagConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That divs with class 'full' is removed") {
    val initialContent = "<article><div class='full'><h1>heading</h1>A small loan of a million dollars</div></article>"
    val expectedResult = "<article> <h1>heading</h1>A small loan of a million dollars</article>"
    val (element, requiredLibraries, errors) = SimpleTagConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }
}
