/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.articleapi.integration.LanguageContent
import no.ndla.articleapi.model._
import org.mockito.Mockito._

import scala.util.{Failure, Try}

class GeneralContentConverterTest extends UnitSuite with TestEnvironment {
  val (nodeId, nodeId2) = ("1234", "4321")
  val insertion = "inline"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val sampleFagstoff1 = NodeGeneralContent(nodeId, nodeId, "Tittel", "Innhold", "nb")
  val sampleFagstoff2 = NodeGeneralContent(nodeId, nodeId2, "Tittel", "Innhald", "nn")

  val generalContentConverter = new GeneralContentConverter {
    override val typeName: String = "test"
  }

  test("That GeneralContentConverter returns the contents according to language") {
    val content = ContentBrowser(contentString, Some("nb"))

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    val (result, requiredLibraries, status) = generalContentConverter.convert(content, Seq())

    result should equal (sampleFagstoff1.content)
    status.messages.isEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That GeneralContentConverter returns an error when the node is not found") {
    val content = ContentBrowser(contentString, Some("nb"))

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq())
    val (result, requiredLibraries, status) = generalContentConverter.convert(content, Seq())

    result should equal (s"{Import error: Failed to retrieve '${generalContentConverter.typeName}' with language 'nb' ($nodeId)}")
    status.messages.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That GeneralContentConverter inserts the content if insertion mode is 'collapsed_body'") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=collapsed_body==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"<details><summary>Tittel</summary>${sampleFagstoff1.content}</details>"

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    when(articleRepository.withExternalId(nodeId)).thenReturn(Some(ArticleSummary("1", "title", "http://url", "publicdomain")))
    val (result, requiredLibraries, status) = generalContentConverter.convert(content, Seq())
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }


  test("That GeneralContentConverter inserts the content if insertion mode is 'link'") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=link==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"""<figure data-resource="content-link" data-id="1" data-content-id="1" data-link-text="Tittel"></figure>"""

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    when(articleRepository.withExternalId(nodeId)).thenReturn(Some(ArticleSummary("1", "title", "http://url", "publicdomain")))
    val (result, requiredLibraries, status) = generalContentConverter.convert(content, Seq())
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That GeneralContentConverter inserts the content if insertion mode is 'lightbox_large'") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=lightbox_large==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"""<figure data-resource="content-link" data-id="1" data-content-id="1" data-link-text="Tittel"></figure>"""

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    when(articleRepository.withExternalId(nodeId)).thenReturn(Some(ArticleSummary("1", "title", "http://url", "publicdomain")))
    val (result, requiredLibraries, status) = generalContentConverter.convert(content, Seq())
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.messages.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That GeneralContentConverter defaults to 'link' if the insertion method is unknown") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=lightbox_large==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"""<figure data-resource="content-link" data-id="1" data-content-id="1" data-link-text="Tittel"></figure>"""

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    when(articleRepository.withExternalId(nodeId)).thenReturn(Some(ArticleSummary("1", "title", "http://url", "publicdomain")))

    val (result, requiredLibraries, status) = generalContentConverter.convert(content, Seq())
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.messages.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That GeneralContentConverter imports nodes from old NDLA which is referenced in a content") {
    val newNodeid: Long = 1111
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=link==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"""<figure data-resource="content-link" data-id="1" data-content-id="$newNodeid" data-link-text="Tittel"></figure>"""

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    when(articleRepository.withExternalId(nodeId)).thenReturn(None)
    when(extractConvertStoreContent.processNode(nodeId, ImportStatus(Seq(), Seq(nodeId2)))).thenReturn(Try((newNodeid, ImportStatus(Seq(), Seq(nodeId2, nodeId)))))

    val languageContent = LanguageContent(nodeId, nodeId, "<div>sample content</div>", Some("en"))
    val nodeToConvert = NodeToConvert(Seq(ArticleTitle("title", Some("en"))), Seq(languageContent), Copyright(License("publicdomain", "public", None), "", Seq()), Seq(), Seq(), Seq(), Seq(), Seq(), 0, 1)
    val (result, requiredLibraries, status) = generalContentConverter.convert(content, Seq(nodeId2))
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult should equal(expectedResult)
    status should equal (ImportStatus(List(), List(nodeId2, nodeId)))
  }

  test("That GeneralContentConverter links back to old NDLA if node could not be imported") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=link==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"""<a href="http://ndla.no//node/$nodeId">Tittel</a>"""

    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleFagstoff1, sampleFagstoff2))
    when(articleRepository.withExternalId(nodeId)).thenReturn(None)
    when(extractConvertStoreContent.processNode(nodeId, ImportStatus(Seq(), Seq(nodeId2)))).thenReturn(Failure(NodeNotFoundException("Node was not found")))

    val languageContent = LanguageContent(nodeId, nodeId2, "<div>sample content</div>", Some("en"))
    val nodeToConvert = NodeToConvert(Seq(ArticleTitle("title", Some("en"))), Seq(languageContent), Copyright(License("publicdomain", "public", None), "", Seq()), Seq(), Seq(), Seq(), Seq(), Seq(), 0, 1)

    val (result, requiredLibraries, status) = generalContentConverter.convert(content, Seq(nodeId2))
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult should equal(expectedResult)
    status.visitedNodes should equal (List(nodeId2))
    status.messages.nonEmpty should equal(true)
  }
}
