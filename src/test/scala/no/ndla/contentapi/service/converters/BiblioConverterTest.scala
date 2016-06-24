package no.ndla.contentapi.service.converters

import no.ndla.contentapi.integration.{Biblio, BiblioAuthor, LanguageContent}
import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._

/**
  * <a id="biblio-1234"></a>
  */
class BiblioConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val sampleBiblio = Biblio("The Catcher in the Rye", "Book", "1951", "1", "Little, Brown and Company")
  val sampleBiblioAuthors = Seq(BiblioAuthor("J. D. Salinger", "Salinger", "Jerome David"))

  test("That BiblioConverter initializes empty references and generates a reference list at the end of the document") {
    val initialContent = LanguageContent(s"""<article><a id="biblio-$nodeId"></a><h1>CONTENT</h1>more content</article>""", Some("en"))
    val expectedResult =
      s"""<article>
         | <a id=\"reference_1\" href=\"#reference_list-$nodeId\">1</a>
         | <h1>CONTENT</h1>more content
         | <div> <ul>
         | <li id=\"reference_list-$nodeId\">
         |<a href=\"#reference_1\">1.</a> ${sampleBiblio.title} (${sampleBiblio.year}), ${sampleBiblioAuthors.map(x => x.name).mkString(",")}
         |, Edition: ${sampleBiblio.edition}, Publisher: ${sampleBiblio.publisher} </li>
         | </ul> </div></article>""".stripMargin.replace("\n", "")

    when(extractService.getBiblio(nodeId)).thenReturn(Some(sampleBiblio))
    when(extractService.getBiblioAuthors(nodeId)).thenReturn(sampleBiblioAuthors)
    val (result, status) = biblioConverter.convert(initialContent)
    val strippedContent = " +".r.replaceAllIn(result.content.replace("\n", ""), " ")

    strippedContent should equal (expectedResult)
    status.messages.isEmpty should be (true)
  }
}
