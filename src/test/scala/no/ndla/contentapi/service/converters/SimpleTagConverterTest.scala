package no.ndla.contentapi.service.converters

import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.learningpathapi.UnitSuite
import org.jsoup.Jsoup

import scala.collection.mutable.ListBuffer

class SimpleTagConverterTest extends UnitSuite {
  test("That divs with class 'paragraph' are replaced with section") {
    val initialContent = "<article><h1>heading</h1><div class='paragraph'>I know words, I have the best words.</div></article>"
    val expectedResult = "<article> <h1>heading</h1> <section>  I know words, I have the best words. </section></article>"
    implicit val requiredLibraries = ListBuffer[RequiredLibrary]()
    val element = SimpleTagConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That divs with class 'full' are removed") {
    val initialContent = "<article><div class='full'><h1>heading</h1>A small loan of a million dollars</div></article>"
    val expectedResult = "<article> <h1>heading</h1>A small loan of a million dollars</article>"
    implicit val requiredLibraries = ListBuffer[RequiredLibrary]()
    val element = SimpleTagConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That children of pre tags are wrapped in code tags") {
    val initialContent = "<article><h1>heading</h1><pre>I know words, I have the best words.</pre></article>"
    val expectedResult = "<article> <h1>heading</h1> <pre><code>I know words, I have the best words.</code></pre></article>"
    implicit val requiredLibraries = ListBuffer[RequiredLibrary]()
    val element = SimpleTagConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That divs with class 'quote' are replaced with a blockquote tag") {
    val initialContent = """<article><h1>heading</h1><div class="quote">I know words, I have the best words.</div></article>"""
    val expectedResult = "<article> <h1>heading</h1> <blockquote>  I know words, I have the best words. </blockquote></article>"
    implicit val requiredLibraries = ListBuffer[RequiredLibrary]()
    val element = SimpleTagConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }
}
