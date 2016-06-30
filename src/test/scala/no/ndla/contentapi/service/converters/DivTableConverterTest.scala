package no.ndla.contentapi.service.converters

import no.ndla.contentapi.integration.LanguageContent
import no.ndla.contentapi.UnitSuite

class DivTableConverterTest extends UnitSuite {

  test("That divs with class 'ndla_table' is converted to table") {
    val initialContent = LanguageContent("<article><div class=\"ndla_table another_class\">nobody builds walls better than me, believe me</div></article>", Some("en"))
    val expectedResult = "<article> <table class=\"ndla_table\">  nobody builds walls better than me, believe me </table></article>"
    val (result, status) = DivTableConverter.convert(initialContent)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'ndla_table_row' is converted to tr") {
    val initialContent = LanguageContent("<article><div class=\"ndla_table_row another_class\">My IQ is one of the highest - and you all know it!</div></article>", Some("en"))
    val expectedResult = "<article> <tr class=\"ndla_table_row\">  My IQ is one of the highest - and you all know it! </tr></article>"
    val (result, status) = DivTableConverter.convert(initialContent)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'ndla_table_cell' is converted to td") {
    val initialContent = LanguageContent("<article><div class=\"ndla_table_cell another_class\">I am very highly educated</div></article>", Some("en"))
    val expectedResult = "<article> <td class=\"ndla_table_cell\">I am very highly educated</td></article>"
    val (result, status) = DivTableConverter.convert(initialContent)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That divs with class 'ndla_table_cell_content' is removed") {
    val initialContent = LanguageContent("<article><div><div class=\"ndla_table_cell_content another_class\">in that wall we are going to have a big fat door</div></div></article>", Some("en"))
    val expectedResult = "<article> <div>  in that wall we are going to have a big fat door </div></article>"
    val (result, status) = DivTableConverter.convert(initialContent)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }
}
