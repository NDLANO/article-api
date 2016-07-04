package no.ndla.contentapi.service.converters

import no.ndla.contentapi.integration.{Biblio, BiblioAuthor, LanguageContent}
import no.ndla.contentapi.model.FootNoteItem
import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._

class BiblioConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val sampleBiblio = Biblio("The Catcher in the Rye", "Book", "1951", "1", "Little, Brown and Company")
  val sampleBiblioAuthors = Seq(BiblioAuthor("J. D. Salinger", "Salinger", "Jerome David"))

  test("That BiblioConverter initializes and builds a map of footnote items") {
    val initialContent = LanguageContent(nodeId, nodeId, s"""<article><a id="biblio-$nodeId"></a><h1>CONTENT</h1>more content</article>""", Some("en"))
    val expectedFootNotes = Map("ref_1" -> FootNoteItem(sampleBiblio, sampleBiblioAuthors))
    val expectedResult = initialContent.copy(content=s"""<article> <a id="ref_1">1</a> <h1>CONTENT</h1>more content</article>""", footNotes=expectedFootNotes)

    when(extractService.getBiblio(nodeId)).thenReturn(Some(sampleBiblio))
    when(extractService.getBiblioAuthors(nodeId)).thenReturn(sampleBiblioAuthors)
    val (result, status) = biblioConverter.convert(initialContent)
    val strippedContent = " +".r.replaceAllIn(result.content.replace("\n", ""), " ")

    result.copy(content=strippedContent) should equal (expectedResult)
    status.messages.isEmpty should be (true)
  }
}
