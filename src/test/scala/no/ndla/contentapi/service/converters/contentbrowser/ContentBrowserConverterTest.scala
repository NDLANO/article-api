package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import no.ndla.contentapi.model.{Copyright, License}
import no.ndla.contentapi.integration._
import no.ndla.contentapi.service.{Image, ImageMetaInformation, ImageVariants}
import org.mockito.Mockito._

class ContentBrowserConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val sampleAlt = "Fotografi"
  val sampleContentString = s"[contentbrowser ==nid=${nodeId}==imagecache=Fullbredde==width===alt=$sampleAlt==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=inline==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val sampleBiblio = Biblio("The Catcher in the Rye", "Book", "1951", "1", "Little, Brown and Company")
  val sampleBiblioAuthors = Seq(BiblioAuthor("J. D. Salinger", "Salinger", "Jerome David"))


  test("That content-browser strings are replaced") {
    val initialContent = LanguageContent(nodeId, nodeId, s"<article><p>$sampleContentString</p></article>", Some("en"))
    val expectedResult = s"<article> <p>{Unsupported content unsupported type: ${nodeId}}</p></article>"

    when(extractService.getNodeType(nodeId)).thenReturn(Some("unsupported type"))
    val (result, status) = contentBrowserConverter.convert(initialContent)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That content-browser strings of type h5p_content are converted correctly") {
    val nodeId = "1234"
    val initialContent = LanguageContent(nodeId, nodeId, s"<article>$sampleContentString</article>", Some("en"))
    val expectedResult = s"""<article> <iframe src="http://ndla.no/h5p/embed/${nodeId}"></iframe></article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("h5p_content"))
    val (result, status) = contentBrowserConverter.convert(initialContent)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (1)
  }

  test("That Content-browser strings of type image are converted into HTML img tags") {
    val (nodeId, imageUrl, alt) = ("1234", "full.jpeg", "Fotografi")
    val initialContent = LanguageContent(nodeId, nodeId, s"<article><p>$sampleContentString</p></article>", Some("en"))
    val expectedResult = s"""<article> <p><img src="/images/${imageUrl}" alt="${alt}" /></p></article>"""
    val imageMeta = Some(ImageMetaInformation("1", List(), List(), ImageVariants(Some(Image("small.jpeg", 128, "")), Some(Image(imageUrl, 256, ""))), Copyright(License("", "", Some("")), "", List()), List()))

    when(extractService.getNodeType(nodeId)).thenReturn((Some("image")))
    when(imageApiService.getMetaByExternId(nodeId)).thenReturn(imageMeta)
    val (result, status) = contentBrowserConverter.convert(initialContent)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That Content-browser strings of type oppgave are converted into content") {
    val initialContent = LanguageContent(nodeId, nodeId, s"<article>$sampleContentString</article>", Some("no"))
    val contentTitle = "Oppgave title"
    val content = """<div class="paragraph">   Very important oppgave text  </div>"""
    val oppgave = ContentOppgave(nodeId, nodeId, contentTitle, content, "no")
    val expectedResult = s"""<article> $content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("oppgave"))
    when(extractService.getNodeOppgave(nodeId)).thenReturn(List(oppgave))
    val (result, status) = contentBrowserConverter.convert(initialContent)

    result.content.replace("\n", "") should equal (expectedResult)
  }


  test("That Content-browser strings of type fagstoff are converted into content") {
    val initialContent = LanguageContent(nodeId, nodeId, s"<article>$sampleContentString</article>", Some("no"))
    val contentTitle = "Fasgtoff title"
    val content = """<div class="paragraph">   Very important fagstoff text  </div>"""
    val oppgave = ContentFagstoff(nodeId, nodeId, contentTitle, content, "no")
    val expectedResult = s"""<article> $content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("fagstoff"))
    when(extractService.getNodeFagstoff(nodeId)).thenReturn(List(oppgave))
    val (result, status) = contentBrowserConverter.convert(initialContent)

    result.content.replace("\n", "") should equal (expectedResult)
  }

  test("That Content-browser strings of type aktualitet are converted into content") {
    val initialContent = LanguageContent(nodeId, nodeId, s"<article>$sampleContentString</article>", Some("no"))
    val contentTitle = "Aktualitet title"
    val content = """<div class="paragraph">   Very important aktualitet text  </div>"""
    val oppgave = ContentFagstoff(nodeId, nodeId, contentTitle, content, "no")
    val expectedResult = s"""<article> $content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("fagstoff"))
    when(extractService.getNodeFagstoff(nodeId)).thenReturn(List(oppgave))
    val (result, status) = contentBrowserConverter.convert(initialContent)

    result.content.replace("\n", "") should equal (expectedResult)
  }

  test("That content-browser strings of type biblio are converted into content") {
    val initialContent = LanguageContent(nodeId, nodeId, s"""<article>$sampleContentString</a><h1>CONTENT</h1>more content</article>""", Some("en"))
    val expectedResult = s"""<article> <a id="biblio-$nodeId"></a> <h1>CONTENT</h1>more content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("biblio"))
    val (result, status) = contentBrowserConverter.convert(initialContent)
    val strippedContent = " +".r.replaceAllIn(result.content.replace("\n", ""), " ")

    strippedContent should equal (expectedResult)
    status.messages.isEmpty should be (true)
  }
}
