package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.TestEnvironment
import no.ndla.contentapi.integration.LanguageContent
import no.ndla.contentapi.model.{Content, Copyright, License}
import no.ndla.contentapi.service.converters.SimpleTagConverter
import no.ndla.contentapi.service.{Image, ImageMetaInformation, ImageVariants}
import no.ndla.learningpathapi.UnitSuite
import org.jsoup.Jsoup
import org.mockito.Mockito._

class ContentBrowserConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val sampleAlt = "Fotografi"
  val sampleContentString = s"[contentbrowser ==nid=${nodeId}==imagecache=Fullbredde==width===alt=$sampleAlt==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"


  test("That content-browser strings are replaced") {
    val initialContent = LanguageContent(s"<article><p>$sampleContentString</p></article>", Some("en"))
    val expectedResult = s"<article> <p>{Unsupported content unsupported type: ${nodeId}}</p></article>"

    when(extractService.getNodeType(nodeId)).thenReturn(Some("unsupported type"))
    val (result, status) = contentBrowserConverter.convert(initialContent)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That content-browser strings of type h5p_content are converted correctly") {
    val nodeId = "1234"
    val initialContent = LanguageContent(s"<article>$sampleContentString</article>", Some("en"))
    val expectedResult = s"""<article> <iframe src="http://ndla.no/h5p/embed/${nodeId}"></iframe></article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("h5p_content"))
    val (result, status) = contentBrowserConverter.convert(initialContent)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (1)
  }

  test("That Content-browser strings of type image are converted into HTML img tags") {
    val (nodeId, imageUrl, alt) = ("1234", "full.jpeg", "Fotografi")
    val initialContent = LanguageContent(s"<article><p>$sampleContentString</p></article>", Some("en"))
    val expectedResult = s"""<article> <p><img src="/images/${imageUrl}" alt="${alt}" /></p></article>"""
    val imageMeta = Some(ImageMetaInformation("1", List(), List(), ImageVariants(Some(Image("small.jpeg", 128, "")), Some(Image(imageUrl, 256, ""))), Copyright(License("", "", Some("")), "", List()), List()))

    when(extractService.getNodeType(nodeId)).thenReturn((Some("image")))
    when(imageApiService.getMetaByExternId(nodeId)).thenReturn(imageMeta)
    val (result, status) = contentBrowserConverter.convert(initialContent)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }
}
