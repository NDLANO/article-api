/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.ArticleApiProperties._
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.articleapi.integration.{ImageCopyright, ImageLicense, ImageMetaInformation, ImageTag}
import no.ndla.validation.EmbedTagRules.ResourceHtmlEmbedTag
import no.ndla.articleapi.model.domain._
import org.mockito.Mockito._

import scala.util.Success

class ContentBrowserConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val sampleAlt = "Fotografi"
  val sampleContentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$sampleAlt==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=inline==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val sampleContent =  TestData.sampleContent.copy(content=s"<article>$sampleContentString</article>")

  test("contentbrowser strings of unsupported causes a Failure to be returned") {
    val expectedResult = s"""<article><$ResourceHtmlEmbedTag data-message="Unsupported content (unsupported type): $nodeId" data-resource="error" /></article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("unsupported type"))
    contentBrowserConverter.convert(sampleContent, ImportStatus.empty).isFailure should be (true)
  }

  test("That Content-browser strings of type image are converted into HTML img tags") {
    val (nodeId, imageUrl, alt) = ("1234", "full.jpeg", "Fotografi")
    val newId = "1"
    val imageMeta = ImageMetaInformation(newId, List(), List(), imageUrl, 256, "", ImageCopyright(ImageLicense("", "", Some("")), "", List()), ImageTag(List(), None))
    val expectedResult =
      s"""|<article>
          |<$ResourceHtmlEmbedTag data-align="" data-alt="$alt" data-caption="" data-resource="image" data-resource_id="1" data-size="fullbredde">
          |</article>""".stripMargin.replace("\n", "")

    when(extractService.getNodeType(nodeId)).thenReturn(Some("image"))
    when(imageApiClient.importImage(nodeId)).thenReturn(Some(imageMeta))
    val Success((result, _)) = contentBrowserConverter.convert(sampleContent, ImportStatus.empty)

    result.content should equal (expectedResult)
    result.requiredLibraries.size should equal (0)
  }

  test("That Content-browser strings of type oppgave are converted into content") {
    val contentTitle = "Oppgave title"
    val content = """<div class="paragraph">   Very important oppgave text  </div>"""
    val oppgave = NodeGeneralContent(nodeId, nodeId, contentTitle, content, "en")
    val expectedResult = s"""<article>$content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("oppgave"))
    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(List(oppgave))
    val Success((result, _)) = contentBrowserConverter.convert(sampleContent, ImportStatus.empty)

    result.content should equal (expectedResult)
  }

  test("That Content-browser strings of type fagstoff are converted into content") {
    val contentTitle = "Fasgtoff title"
    val content = """<div class="paragraph">   Very important fagstoff text  </div>"""
    val oppgave = NodeGeneralContent(nodeId, nodeId, contentTitle, content, "en")
    val expectedResult = s"""<article>$content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("fagstoff"))
    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(List(oppgave))
    val Success((result, _)) = contentBrowserConverter.convert(sampleContent, ImportStatus.empty)

    result.content should equal (expectedResult)
  }

  test("That Content-browser strings of type aktualitet are converted into content") {
    val contentTitle = "Aktualitet title"
    val content = """<div class="paragraph">   Very important aktualitet text  </div>"""
    val oppgave = NodeGeneralContent(nodeId, nodeId, contentTitle, content, "en")
    val expectedResult = s"""<article>$content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("aktualitet"))
    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(List(oppgave))
    val Success((result, _)) = contentBrowserConverter.convert(sampleContent, ImportStatus.empty)

    result.content should equal (expectedResult)
  }

  test("That Content-browser strings of type veiledning are converted into content") {
    val contentTitle = "Veiledning title"
    val content = """<div class="paragraph">   Very important veiledning text  </div>"""
    val oppgave = NodeGeneralContent(nodeId, nodeId, contentTitle, content, "en")
    val expectedResult = s"""<article>$content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("veiledning"))
    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(List(oppgave))
    val Success((result, _)) = contentBrowserConverter.convert(sampleContent, ImportStatus.empty)

    result.content should equal (expectedResult)
  }

  test("That Content-browser strings of type video are converted into HTML img tags") {
    val expectedResult = s"""<article><$ResourceHtmlEmbedTag data-account="$NDLABrightcoveAccountId" data-caption="" data-player="$NDLABrightcovePlayerId" data-resource="brightcove" data-videoid="ref:$nodeId"></article>"""
    when(extractService.getNodeType(nodeId)).thenReturn(Some("video"))
    val Success((result, _)) = contentBrowserConverter.convert(sampleContent, ImportStatus.empty)
    val strippedResult = " +".r.replaceAllIn(result.content.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    result.requiredLibraries.size should equal (0)
  }

  test("That content-browser strings of type biblio are converted into content") {
    val initialContent = sampleContent.copy(content=s"""<article>$sampleContentString</a><h1>CONTENT</h1>more content</article>""")
    val biblio = BiblioMeta(Biblio("title", "book", "2009", "1", "me"), Seq(BiblioAuthor("first last", "last", "first")))
    val expectedResult = s"""<article><$ResourceHtmlEmbedTag data-authors="${biblio.authors.head.name}" data-edition="${biblio.biblio.edition}" data-publisher="${biblio.biblio.publisher}" data-resource="footnote" data-title="${biblio.biblio.title}" data-type="${biblio.biblio.bibType}" data-year="${biblio.biblio.year}"><h1>CONTENT</h1>more content</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("biblio"))
    when(extractService.getBiblioMeta(nodeId)).thenReturn(Some(biblio))
    val Success((result, status)) = contentBrowserConverter.convert(initialContent, ImportStatus.empty)
    val strippedContent = " +".r.replaceAllIn(result.content, " ")

    strippedContent should equal (expectedResult)
    status.messages.isEmpty should be (true)
  }

  test("meta description is converted") {
    val metaDescription = """<div class="paragraph">   Very important aktualitet text  </div>"""
    val oppgave = NodeGeneralContent(nodeId, nodeId, "Aktualitet title", metaDescription, "en")
    val expectedResult = s"""<article>$metaDescription</article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("aktualitet"))
    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(List(oppgave))
    val Success((result, _)) = contentBrowserConverter.convert(sampleContent.copy(content="", metaDescription=sampleContent.content), ImportStatus.empty)

    result.content should equal ("")
    result.metaDescription should equal (expectedResult)
  }
}
