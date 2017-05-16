/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import java.util.Date

import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.model.api.NotFoundException
import no.ndla.articleapi.model.domain._
import org.mockito.Mockito._

import scala.util.{Failure, Success, Try}

class GeneralContentConverterTest extends UnitSuite with TestEnvironment {
  val (nodeId, nodeId2) = ("1234", "4321")
  val insertion = "inline"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val sampleFagstoff1 = NodeGeneralContent(nodeId, nodeId, "Tittel", "Innhold", "nb")
  val sampleFagstoff2 = NodeGeneralContent(nodeId, nodeId2, "Tittel", "Innhald", "nn")
  val sampleArticleSummary = ArticleSummary(1, Seq(ArticleTitle("title", Some("nb"))), Seq(), Seq(), "http://url", "publicdomain")
  val sampleNodeToConvert = NodeToConvert(Seq(ArticleTitle("title", Some("en"))), Seq(), "publicdomain", Seq(), Seq(), Seq(), "fagstoff", new Date(0), new Date(1), ArticleType.Standard)
  val sampleContent = TestData.sampleContent.copy(content="<div>sample content</div>")

  val generalContentConverter = new GeneralContentConverter {
    override val typeName: String = "test"
  }

  test("That GeneralContentConverter returns the contents according to language") {
    val content = ContentBrowser(contentString, Some("nb"))

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    val Success((result, requiredLibraries, status)) = generalContentConverter.convert(content, Seq())

    result should equal (sampleFagstoff1.content)
    status.messages.isEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That GeneralContentConverter returns a Failure when the node is not found") {
    val content = ContentBrowser(contentString, Some("nb"))

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq())
    generalContentConverter.convert(content, Seq()).isFailure should be (true)
  }

  test("That GeneralContentConverter inserts the content if insertion mode is 'collapsed_body'") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=collapsed_body==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"<details><summary>Tittel</summary>${sampleFagstoff1.content}</details>"

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    when(articleRepository.getIdFromExternalId(nodeId)).thenReturn(Some(1: Long))
    val Success((result, requiredLibraries, status)) = generalContentConverter.convert(content, Seq())

    result should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }


  test("That GeneralContentConverter inserts the content if insertion mode is 'link'") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=link==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s""" <$resourceHtmlEmbedTag data-content-id="1" data-link-text="Tittel" data-resource="content-link" />"""

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    when(articleRepository.getIdFromExternalId(nodeId)).thenReturn(Some(1: Long))
    val Success((result, requiredLibraries, status)) = generalContentConverter.convert(content, Seq())

    result should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That GeneralContentConverter inserts the content if insertion mode is 'lightbox_large'") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=lightbox_large==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s""" <$resourceHtmlEmbedTag data-content-id="1" data-link-text="Tittel" data-resource="content-link" />"""

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    when(articleRepository.getIdFromExternalId(nodeId)).thenReturn(Some(1: Long))
    val Success((result, requiredLibraries, status)) = generalContentConverter.convert(content, Seq())

    result should equal (expectedResult)
    status.messages.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That GeneralContentConverter defaults to 'link' if the insertion method is unknown") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=lightbox_large==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s""" <$resourceHtmlEmbedTag data-content-id="1" data-link-text="Tittel" data-resource="content-link" />"""

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    when(articleRepository.getIdFromExternalId(nodeId)).thenReturn(Some(1: Long))

    val Success((result, requiredLibraries, status)) = generalContentConverter.convert(content, Seq())

    result should equal (expectedResult)
    status.messages.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That GeneralContentConverter imports nodes from old NDLA which is referenced in a content") {
    val newNodeid: Long = 1111
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=link==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s""" <$resourceHtmlEmbedTag data-content-id="1111" data-link-text="Tittel" data-resource="content-link" />"""

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    when(articleRepository.getIdFromExternalId(nodeId)).thenReturn(None)
    when(extractConvertStoreContent.processNode(nodeId, ImportStatus(Seq(), Seq(nodeId2)))).thenReturn(Try((newNodeid, ImportStatus(Seq(), Seq(nodeId2, nodeId)))))

    val languageContent = sampleContent.copy(content="<div>sample content</div>")
    val Success((result, _, status)) = generalContentConverter.convert(content, Seq(nodeId2))

    result should equal(expectedResult)
    status should equal (ImportStatus(List(), List(nodeId2, nodeId)))
  }

  test("That GeneralContentConverter returns a Failure if node could not be imported") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=link==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s""" <a href="http://ndla.no/node/$nodeId" title="">Tittel</a>"""

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    when(articleRepository.getIdFromExternalId(nodeId)).thenReturn(None)
    when(extractConvertStoreContent.processNode(nodeId, ImportStatus(Seq(), Seq(nodeId2)))).thenReturn(Failure(NotFoundException("Node was not found")))

    generalContentConverter.convert(content, Seq(nodeId2)).isFailure should be (true)
  }
}
