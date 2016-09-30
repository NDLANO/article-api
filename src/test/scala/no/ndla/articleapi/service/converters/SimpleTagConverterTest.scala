/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters

import no.ndla.articleapi.UnitSuite
import no.ndla.articleapi.integration.LanguageContent
import no.ndla.articleapi.model.ImportStatus

class SimpleTagConverterTest extends UnitSuite {
  val nodeId = "1234"

  test("That divs with class 'paragraph' are replaced with section") {
    val initialContent = LanguageContent(nodeId, nodeId, "<h1>heading</h1><div class='paragraph'>I know words, I have the best words.</div>", Some("en"))
    val expectedResult = "<h1>heading</h1><section>I know words, I have the best words.</section>"
    val (result, status) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'full' are removed") {
    val initialContent = LanguageContent(nodeId, nodeId, "<article><div class='full'><h1>heading</h1>A small loan of a million dollars</div></article>", Some("en"))
    val expectedResult = "<article><h1>heading</h1>A small loan of a million dollars</article>"
    val (result, status) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That children of pre tags are wrapped in code tags") {
    val initialContent = LanguageContent(nodeId, nodeId, "<h1>heading</h1><pre>I know words, I have the best words.</pre>", Some("en"))
    val expectedResult = "<h1>heading</h1><pre><code>I know words, I have the best words.</code></pre>"
    val (result, status) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'quote' are replaced with a blockquote tag") {
    val initialContent = LanguageContent(nodeId, nodeId, """<article><h1>heading</h1><div class="quote">I know words, I have the best words.</div></article>""", Some("en"))
    val expectedResult = "<article><h1>heading</h1><blockquote>I know words, I have the best words.</blockquote></article>"
    val (result, status) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'right' are replaced with a aside tag") {
    val initialContent = LanguageContent(nodeId, nodeId, """<article><div class="right">I know words, I have the best words.</div></article>""", Some("en"))
    val expectedResult = "<article><aside>I know words, I have the best words.</aside></article>"
    val (result, status) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'hide' converted to details-summary tags") {
    val initialContent = LanguageContent(nodeId, nodeId, """<div class="hide">Eksempel: <a href="#" class="read-more">les mer</a>
      <div class="details">
        <p>Hello, this is content</p>
        <a class="re-collapse" href="#">skjul</a>
      </div>
    </div>""", Some("en"))
    val expectedResult = "<details><summary>Eksempel: les mer</summary><p>Hello, this is content</p></details>"
    val (result, status) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That body is converted to article") {
    val initialContent = LanguageContent(nodeId, nodeId, """<body><div class="right">I know words, I have the best words.</div></body>""", Some("en"))
    val expectedResult = "<aside>I know words, I have the best words.</aside>"
    val (result, status) = SimpleTagConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

}
