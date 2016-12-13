/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters

import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._

class BiblioConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val sampleBiblio = Biblio("The Catcher in the Rye", "Book", "1951", "1", "Little, Brown and Company")
  val sampleBiblioAuthors = Seq(BiblioAuthor("J. D. Salinger", "Salinger", "Jerome David"))
  val sampleBiblioMeta = BiblioMeta(sampleBiblio, sampleBiblioAuthors)

  test("That BiblioConverter initializes and builds a map of footnote items") {
    val initialContent = TestData.sampleContent.copy(content=s"""<article><a id="biblio-$nodeId"></a><h1>CONTENT</h1>more content</article>""")
    val expectedFootNotes = Map("ref_1" -> FootNoteItem(sampleBiblio, sampleBiblioAuthors))
    val expectedResult = initialContent.copy(content=s"""<article><a data-resource="footnote" data-key="ref_1"><sup>1</sup></a><h1>CONTENT</h1>more content</article>""", footNotes=Some(expectedFootNotes))

    when(extractService.getBiblioMeta(nodeId)).thenReturn(Some(sampleBiblioMeta))
    val (result, status) = biblioConverter.convert(initialContent, ImportStatus(Seq(), Seq()))
    val strippedContent = " +".r.replaceAllIn(result.content, " ")

    result.copy(content=strippedContent) should equal (expectedResult)
    status.messages.isEmpty should be (true)
  }
}
