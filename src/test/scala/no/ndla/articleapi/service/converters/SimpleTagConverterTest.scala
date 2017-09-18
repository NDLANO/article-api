/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters

import no.ndla.articleapi.integration.LanguageContent
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
    result.requiredLibraries.size should equal (0)
  }

  test("That divs with class 'full' are removed") {
    val initialContent = TestData.sampleContent.copy(content="<article><div class='full'><h1>heading</h1>A small loan of a million dollars</div></article>")
    val expectedResult = "<article><h1>heading</h1>A small loan of a million dollars</article>"
    val Success((result, _)) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.size should equal (0)
  }

  test("That children of pre tags are wrapped in code tags") {
    val initialContent = TestData.sampleContent.copy(content="<h1>heading</h1><pre>I know words, I have the best words.</pre>")
    val expectedResult = "<h1>heading</h1><pre><code>I know words, I have the best words.</code></pre>"
    val Success((result, _)) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.size should equal (0)
  }

  test("That divs with class 'quote' are replaced with a blockquote tag") {
    val initialContent = TestData.sampleContent.copy(content="""<article><h1>heading</h1><div class="quote">I know words, I have the best words.</div></article>""")
    val expectedResult = "<article><h1>heading</h1><blockquote>I know words, I have the best words.</blockquote></article>"
    val Success((result, _)) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.size should equal (0)
  }

  test("That divs with class 'right' are replaced with a aside tag") {
    val initialContent = TestData.sampleContent.copy(content="""<article><div class="right">I know words, I have the best words.</div></article>""")
    val expectedResult = "<article><aside>I know words, I have the best words.</aside></article>"
    val Success((result, _)) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.size should equal (0)
  }

  test("That divs with class 'hide' converted to details-summary tags") {
    val initialContent = TestData.sampleContent.copy(content="""<div class="hide">Eksempel: <a href="#" class="read-more">les mer</a>
          |<div class="details">
            |<p>Hello, this is content</p>
            |<a class="re-collapse" href="#">skjul</a>
          |</div>
        |</div>""".stripMargin.replace("\n", ""))
    val expectedResult = "<details><summary>Eksempel: les mer</summary><p>Hello, this is content</p></details>"
    val Success((result, _)) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.size should equal (0)
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
    result.requiredLibraries.size should equal (0)
  }

  test("That divs with class 'ndla_table' is converted to table") {
    val sampleLanguageContent: LanguageContent = TestData.sampleContent.copy(content="<article><div class=\"ndla_table another_class\">nobody builds walls better than me, believe me</div></article>")
    val expectedResult = "<article><table>nobody builds walls better than me, believe me</table></article>"
    val result = SimpleTagConverter.convert(sampleLanguageContent, ImportStatus(Seq(), Seq()))
    result.isSuccess should be (true)

    val Success((content, _)) = result

    content.content should equal (expectedResult)
    content.requiredLibraries.size should equal (0)
  }

  test("That divs with class 'ndla_table_row' is converted to tr") {
    val initialContent: LanguageContent = TestData.sampleContent.copy(content="<article><div class=\"ndla_table_row another_class\">My IQ is one of the highest - and you all know it!</div></article>")
    val expectedResult = "<article><tr>My IQ is one of the highest - and you all know it!</tr></article>"
    val result = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))
    result.isSuccess should be (true)

    val Success((content, _)) = result
    content.content should equal (expectedResult)
    content.requiredLibraries.size should equal (0)
  }

  test("That divs with class 'ndla_table_cell' is converted to td") {
    val initialContent: LanguageContent = TestData.sampleContent.copy(content="<article><div class=\"ndla_table_cell another_class\">I am very highly educated</div></article>")
    val expectedResult = "<article><td>I am very highly educated</td></article>"
    val result = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))
    result.isSuccess should be (true)

    val Success((content, _)) = result
    content.content should equal (expectedResult)
    content.requiredLibraries.size should equal (0)
  }

  test("That divs with class 'ndla_table_cell_content' is removed") {
    val initialContent: LanguageContent = TestData.sampleContent.copy(content="<article><div><div class=\"ndla_table_cell_content another_class\">in that wall we are going to have a big fat door</div></div></article>")
    val expectedResult = "<article><div>in that wall we are going to have a big fat door</div></article>"
    val result = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))
    result.isSuccess should be (true)

    val Success((content, _)) = result
    content.content should equal (expectedResult)
    content.requiredLibraries.size should equal (0)
  }

}
