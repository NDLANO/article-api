/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters

import no.ndla.articleapi.{TestData, UnitSuite}
import no.ndla.articleapi.model.domain.ImportStatus

import scala.util.Success

class SimpleTagConverterTest extends UnitSuite {
  val nodeId = "1234"

  test("That divs with class 'paragraph' are replaced with section") {
    val sampleLanguageContent = TestData.sampleContent.copy(content="<h1>heading</h1><div class='paragraph'>I know words, I have the best words.</div>")
    val expectedResult = "<h1>heading</h1><section>I know words, I have the best words.</section>"
    val Success((result, _)) = SimpleTagConverter.convert(sampleLanguageContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'full' are removed") {
    val initialContent = TestData.sampleContent.copy(content="<article><div class='full'><h1>heading</h1>A small loan of a million dollars</div></article>")
    val expectedResult = "<article><h1>heading</h1>A small loan of a million dollars</article>"
    val Success((result, _)) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That children of pre tags are wrapped in code tags") {
    val initialContent = TestData.sampleContent.copy(content="<h1>heading</h1><pre>I know words, I have the best words.</pre>")
    val expectedResult = "<h1>heading</h1><pre><code>I know words, I have the best words.</code></pre>"
    val Success((result, _)) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'quote' are replaced with a blockquote tag") {
    val initialContent = TestData.sampleContent.copy(content="""<article><h1>heading</h1><div class="quote">I know words, I have the best words.</div></article>""")
    val expectedResult = "<article><h1>heading</h1><blockquote>I know words, I have the best words.</blockquote></article>"
    val Success((result, _)) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'right' are replaced with a aside tag") {
    val initialContent = TestData.sampleContent.copy(content="""<article><div class="right">I know words, I have the best words.</div></article>""")
    val expectedResult = "<article><aside>I know words, I have the best words.</aside></article>"
    val Success((result, _)) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'hide' converted to details-summary tags") {
    val initialContent = TestData.sampleContent.copy(content="""<div class="hide">Eksempel: <a href="#" class="read-more">les mer</a>
      <div class="details">
        <p>Hello, this is content</p>
        <a class="re-collapse" href="#">skjul</a>
      </div>
    </div>""")
    val expectedResult = "<details><summary>Eksempel: les mer</summary><p>Hello, this is content</p></details>"
    val Success((result, _)) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'frame' convertet to class c-bodybox"){
    val initialContent = TestData.sampleContent.copy(content = """<div class="frame"><h4>De fire friheter</h4><p>Fri bevegelse av</p><ul><li>varer</li><li>tjenester</li><li>kapital </li><li>personer</li></ul></div>""")
    val expectedResult = """<div class="c-bodybox"><h4>De fire friheter</h4><p>Fri bevegelse av</p><ul><li>varer</li><li>tjenester</li><li>kapital </li><li>personer</li></ul></div>"""
    val Success((result, _)) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
  }

  test("That body is converted to article") {
    val initialContent = TestData.sampleContent.copy(content="""<body><div class="right">I know words, I have the best words.</div></body>""")
    val expectedResult = "<aside>I know words, I have the best words.</aside>"
    val Success((result, _)) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

}
