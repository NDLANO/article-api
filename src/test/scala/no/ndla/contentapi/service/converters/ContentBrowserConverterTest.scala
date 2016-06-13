package no.ndla.contentapi.service.converters

import no.ndla.contentapi.TestEnvironment
import no.ndla.contentapi.integration.ContentOppgave
import no.ndla.contentapi.model.{Copyright, License, RequiredLibrary}
import no.ndla.contentapi.service.{Image, ImageMetaInformation, ImageVariants}
import no.ndla.learningpathapi.UnitSuite
import org.jsoup.Jsoup
import org.mockito.Mockito._

import scala.collection.mutable.ListBuffer

class ContentBrowserConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val currentLanguage = "nb"
  val sampleAltString = "Fotografi"
  val sampleContentString = s"[contentbrowser ==nid=${nodeId}==imagecache=Fullbredde==width===alt=$sampleAltString==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"

  test("That content-browser strings are replaced") {
    val initialContent = s"<article><p>$sampleContentString</p></article>"
    val expectedResult = s"<article> <p>{Unsupported content: $nodeId}</p></article>"

    when(extractService.getNodeType(nodeId)).thenReturn(Some(""))
    val (element, requiredLibraries, errors)  = contentBrowserConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0), currentLanguage)

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That content-browser strings of type h5p_content are converted correctly") {
    val initialContent = s"<article>$sampleContentString</article>"
    val expectedResult = s"""<article> <iframe src="http://ndla.no/h5p/embed/${nodeId}"></iframe></article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("h5p_content"))
    val (element, requiredLibraries, errors) = contentBrowserConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0), currentLanguage)

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (1)
  }

  test("That Content-browser strings of type image are converted into HTML img tags") {
    val imageUrl = "full.jpeg"
    val initialContent = s"<article><p>$sampleContentString</p></article>"
    val expectedResult = s"""<article> <p><img src="/images/${imageUrl}" alt="${sampleAltString}" /></p></article>"""
    val imageMeta = Some(ImageMetaInformation("1", List(), List(), ImageVariants(Some(Image("small.jpeg", 128, "")), Some(Image(imageUrl, 256, ""))), Copyright(License("", "", Some("")), "", List()), List()))

    when(extractService.getNodeType(nodeId)).thenReturn((Some("image")))
    when(imageApiService.getMetaByExternId(nodeId)).thenReturn(imageMeta)
    val (element, requiredLibraries, errors) = contentBrowserConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0), currentLanguage)

    element.outerHtml().replace("\n", "") should equal (expectedResult)
    requiredLibraries.length should equal (0)
  }

  test("That Content-browser strings of type oppgave are converted into content") {
    val initialContent = s"<article>$sampleContentString</article>"
    val contentTitle = "Oppgave title"
    val content = """<div class="paragraph">   Very important oppgave text  </div>"""
    val oppgave = ContentOppgave(nodeId, nodeId, contentTitle, content, currentLanguage)
    val expectedResult = s"""<article> $content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("oppgave"))
    when(extractService.getNodeOppgave(nodeId)).thenReturn(List(oppgave))
    val (element, requiredLibraries, errors) = contentBrowserConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0), currentLanguage)

    printf("actual  : %s\n", element.outerHtml().replace("\n", ""))
    printf("expected: %s\n", expectedResult)

    element.outerHtml().replace("\n", "") should equal (expectedResult)
  }
}
