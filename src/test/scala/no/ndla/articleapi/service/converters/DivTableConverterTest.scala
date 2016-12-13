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

class DivTableConverterTest extends UnitSuite {
  test("That divs with class 'ndla_table' is converted to table") {
    val sampleLanguageContent = TestData.sampleContent.copy(content="<article><div class=\"ndla_table another_class\">nobody builds walls better than me, believe me</div></article>")
    val expectedResult = "<article><table class=\"ndla_table\">nobody builds walls better than me, believe me</table></article>"
    val (result, status) = DivTableConverter.convert(sampleLanguageContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'ndla_table_row' is converted to tr") {
    val initialContent = TestData.sampleContent.copy(content="<article><div class=\"ndla_table_row another_class\">My IQ is one of the highest - and you all know it!</div></article>")
    val expectedResult = "<article><tr class=\"ndla_table_row\">My IQ is one of the highest - and you all know it!</tr></article>"
    val (result, status) = DivTableConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'ndla_table_cell' is converted to td") {
    val initialContent = TestData.sampleContent.copy(content="<article><div class=\"ndla_table_cell another_class\">I am very highly educated</div></article>")
    val expectedResult = "<article><td class=\"ndla_table_cell\">I am very highly educated</td></article>"
    val (result, status) = DivTableConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'ndla_table_cell_content' is removed") {
    val initialContent = TestData.sampleContent.copy(content="<article><div><div class=\"ndla_table_cell_content another_class\">in that wall we are going to have a big fat door</div></div></article>")
    val expectedResult = "<article><div>in that wall we are going to have a big fat door</div></article>"
    val (result, status) = DivTableConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }
}
