package no.ndla.contentapi.service.converters

import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.learningpathapi.UnitSuite
import org.jsoup.Jsoup

import scala.collection.mutable.ListBuffer

class DivTableConverterTest extends UnitSuite {
  test("That divs with class 'ndla_table' is converted to table") {
    val initialContent = "<div class=\"ndla_table another_class\">nobody builds walls better than me, believe me</div>"
    val expectedResult = "<table class=\"ndla_table\"> nobody builds walls better than me, believe me</table>"
    implicit val requiredLibraries = ListBuffer[RequiredLibrary]()
    val content = DivTableConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    content.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That divs with class 'ndla_table_row' is converted to tr") {
    val initialContent = "<div class=\"ndla_table_row another_class\">My IQ is one of the highest - and you all know it!</div>"
    val expectedResult = "<tr class=\"ndla_table_row\"> My IQ is one of the highest - and you all know it!</tr>"
    implicit val requiredLibraries = ListBuffer[RequiredLibrary]()
    val content = DivTableConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    content.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That divs with class 'ndla_table_cell' is converted to td") {
    val initialContent = "<div class=\"ndla_table_cell another_class\">I'm very highly educated</div>"
    val expectedResult = "<td class=\"ndla_table_cell\">I'm very highly educated</td>"
    implicit val requiredLibraries = ListBuffer[RequiredLibrary]()
    val content = DivTableConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    content.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That divs with class 'ndla_table_cell_content' is removed") {
    val initialContent = "<div><div class=\"ndla_table_cell_content another_class\">in that wall we're going to have a big fat door</div></div>"
    val expectedResult = "<div> in that wall we're going to have a big fat door</div>"
    implicit val requiredLibraries = ListBuffer[RequiredLibrary]()
    val content = DivTableConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    content.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }
}
