/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.ArticleApiProperties._
import no.ndla.articleapi.TestEnvironment
import no.ndla.articleapi.model.{Copyright, ImportStatus, License, NodeGeneralContent}
import no.ndla.articleapi.integration.LanguageContent
import no.ndla.articleapi.service.{Image, ImageMetaInformation, ImageVariants}
import no.ndla.articleapi.UnitSuite
import org.mockito.Mockito._

class ContentBrowserConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val sampleAlt = "Fotografi"
  val sampleContentString = s"[contentbrowser ==nid=${nodeId}==imagecache=Fullbredde==width===alt=$sampleAlt==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=inline==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"

  test("That content-browser strings are replaced") {
    val initialContent = LanguageContent(nodeId, nodeId, s"<article><p>$sampleContentString</p></article>", Some("en"))
    val expectedResult = s"<article><p>{Unsupported content unsupported type: ${nodeId}}</p></article>"

    when(extractService.getNodeType(nodeId)).thenReturn(Some("unsupported type"))
    val (result, status) = contentBrowserConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That content-browser strings of type h5p_content are converted correctly") {
    val nodeId = "1234"
    val initialContent = LanguageContent(nodeId, nodeId, s"<article>$sampleContentString</article>", Some("en"))
    val expectedResult = s"""<article><figure data-resource="h5p" data-id="1" data-url="http://ndla.no/h5p/embed/${nodeId}"></figure></article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("h5p_content"))
    val (result, status) = contentBrowserConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.length should equal (1)
  }

  test("That Content-browser strings of type image are converted into HTML img tags") {
    val (nodeId, imageUrl, alt) = ("1234", "full.jpeg", "Fotografi")
    val newId = "1"
    val initialContent = LanguageContent(nodeId, nodeId, s"<article>$sampleContentString</article>", Some("en"))
    val imageMeta = ImageMetaInformation(newId, List(), List(), ImageVariants(Some(Image("small.jpeg", 128, "")), Some(Image(imageUrl, 256, ""))), Copyright(License("", "", Some("")), "", List()), List())
    val expectedResult =
      s"""|<article>
          |<figure data-size="fullbredde" data-url="http://localhost/images/$newId" data-id="1" data-resource="image" data-alt="$alt" data-caption=""></figure>
          |</article>""".stripMargin.replace("\n", "")

    when(extractService.getNodeType(nodeId)).thenReturn(Some("image"))
    when(imageApiService.importOrGetMetaByExternId(nodeId)).thenReturn(Some(imageMeta))
    val (result, status) = contentBrowserConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That Content-browser strings of type oppgave are converted into content") {
    val initialContent = LanguageContent(nodeId, nodeId, s"<article>$sampleContentString</article>", Some("no"))
    val contentTitle = "Oppgave title"
    val content = """<div class="paragraph">   Very important oppgave text  </div>"""
    val oppgave = NodeGeneralContent(nodeId, nodeId, contentTitle, content, "no")
    val expectedResult = s"""<article>$content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("oppgave"))
    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(List(oppgave))
    val (result, status) = contentBrowserConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
  }


  test("That Content-browser strings of type fagstoff are converted into content") {
    val initialContent = LanguageContent(nodeId, nodeId, s"<article>$sampleContentString</article>", Some("no"))
    val contentTitle = "Fasgtoff title"
    val content = """<div class="paragraph">   Very important fagstoff text  </div>"""
    val oppgave = NodeGeneralContent(nodeId, nodeId, contentTitle, content, "no")
    val expectedResult = s"""<article>$content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("fagstoff"))
    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(List(oppgave))
    val (result, status) = contentBrowserConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
  }

  test("That Content-browser strings of type aktualitet are converted into content") {
    val initialContent = LanguageContent(nodeId, nodeId, s"<article>$sampleContentString</article>", Some("no"))
    val contentTitle = "Aktualitet title"
    val content = """<div class="paragraph">   Very important aktualitet text  </div>"""
    val oppgave = NodeGeneralContent(nodeId, nodeId, contentTitle, content, "no")
    val expectedResult = s"""<article>$content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("aktualitet"))
    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(List(oppgave))
    val (result, status) = contentBrowserConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
  }

  test("That Content-browser strings of type veiledning are converted into content") {
    val initialContent = LanguageContent(nodeId, nodeId, s"<article>$sampleContentString</article>", Some("no"))
    val contentTitle = "Veiledning title"
    val content = """<div class="paragraph">   Very important veiledning text  </div>"""
    val oppgave = NodeGeneralContent(nodeId, nodeId, contentTitle, content, "no")
    val expectedResult = s"""<article>$content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("veiledning"))
    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(List(oppgave))
    val (result, status) = contentBrowserConverter.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content should equal (expectedResult)
  }

  test("That Content-browser strings of type video are converted into HTML img tags") {
    val initialContent = LanguageContent(nodeId, nodeId, s"<article>$sampleContentString</article>", Some("en"))
    val expectedResult = s"""<article><figure data-videoid="ref:$nodeId" data-id="1" data-player="$NDLABrightcovePlayerId" data-resource="brightcove" data-account="$NDLABrightcoveAccountId"></figure></article>"""
    when(extractService.getNodeType(nodeId)).thenReturn(Some("video"))
    val (result, status) = contentBrowserConverter.convert(initialContent, ImportStatus(Seq(), Seq()))
    val strippedResult = " +".r.replaceAllIn(result.content.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    result.requiredLibraries.length should equal (1)
  }

  test("That content-browser strings of type biblio are converted into content") {
    val initialContent = LanguageContent(nodeId, nodeId, s"""<article>$sampleContentString</a><h1>CONTENT</h1>more content</article>""", Some("en"))
    val expectedResult = s"""<article><a id="biblio-$nodeId"></a><h1>CONTENT</h1>more content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("biblio"))
    val (result, status) = contentBrowserConverter.convert(initialContent, ImportStatus(Seq(), Seq()))
    val strippedContent = " +".r.replaceAllIn(result.content, " ")

    strippedContent should equal (expectedResult)
    status.messages.isEmpty should be (true)
  }
}
