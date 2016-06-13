package no.ndla.contentapi.service.converters

import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.learningpathapi.UnitSuite
import org.jsoup.Jsoup

import scala.collection.mutable.ListBuffer

class SimpleTagConverterTest extends UnitSuite {
  val currentLanguage = "nb"

  test("That divs with class 'paragraph' are replaced with section") {
    val initialContent = "<article><h1>heading</h1><div class='paragraph'>I know words, I have the best words.</div></article>"
    val expectedResult = "<article> <h1>heading</h1> <section>  I know words, I have the best words. </section></article>"
    val (element, requiredLibraries, errors) = SimpleTagConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0), currentLanguage)

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That divs with class 'full' are removed") {
    val initialContent = "<article><div class='full'><h1>heading</h1>A small loan of a million dollars</div></article>"
    val expectedResult = "<article> <h1>heading</h1>A small loan of a million dollars</article>"
    val (element, requiredLibraries, errors) = SimpleTagConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0), currentLanguage)

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That children of pre tags are wrapped in code tags") {
    val initialContent = "<article><h1>heading</h1><pre>I know words, I have the best words.</pre></article>"
    val expectedResult = "<article> <h1>heading</h1> <pre><code>I know words, I have the best words.</code></pre></article>"
    val (element, requiredLibraries, errors) = SimpleTagConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0), currentLanguage)

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That divs with class 'quote' are replaced with a blockquote tag") {
    val initialContent = """<article><h1>heading</h1><div class="quote">I know words, I have the best words.</div></article>"""
    val expectedResult = "<article> <h1>heading</h1> <blockquote>  I know words, I have the best words. </blockquote></article>"
    val (element, requiredLibraries, errors) = SimpleTagConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0), currentLanguage)

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That divs with class 'right' are replaced with a aside tag") {
    val initialContent = """<article><div class="right">I know words, I have the best words.</div></article>"""
    val expectedResult = "<article> <aside>  I know words, I have the best words. </aside></article>"
    val (element, requiredLibraries, errors) = SimpleTagConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0), currentLanguage)

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That divs with class 'hide' converted to details-summary tags") {
    val initialContent = """<div class="hide">Eksempel: <a href="#" class="read-more">les mer</a>
      <div class="details">
        <p>Hello, this is content</p>
        <a class="re-collapse" href="#">skjul</a>
      </div>
    </div>""""
    val expectedResult = "<details> <summary>Eksempel: les mer</summary> <p>Hello, this is content</p></details>"
    val (element, requiredLibraries, errors) = SimpleTagConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0), currentLanguage)

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }
}
