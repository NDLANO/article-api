/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import no.ndla.articleapi.TestEnvironment
import no.ndla.articleapi.integration.{LanguageContent, MigrationRelatedContent, MigrationRelatedContents}
import no.ndla.articleapi.model._
import no.ndla.articleapi.UnitSuite
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.util.{Success, Try}

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  val service = new ConverterService
  val contentTitle = ArticleTitle("", Some(""))
  val license = License("licence", "description", Some("http://"))
  val author = Author("forfatter", "Henrik")
  val copyright = Copyright(license, "", List(author))
  val tag = ArticleTag(List("asdf"), Some("nb"))
  val pageTitle = PageTitle("Fanetittel", "type", Some("nb"))
  val visualElement = VisualElement("http://image-api/1", "image", Some("nb"))
  val relatedContents = MigrationRelatedContents(Seq(MigrationRelatedContent("4321", "Programmering", ".../#fordypning", 1)), Some("nb"))
  val requiredLibrary = RequiredLibrary("", "", "")
  val nodeId = "1234"
  val sampleAlt = "Fotografi"
  val sampleContentString = s"[contentbrowser ==nid=${nodeId}==imagecache=Fullbredde==width===alt=Foto==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"


  test("That the document is wrapped in an article tag") {
    val nodeId = "1"
    val initialContent = "<h1>Heading</h1>"
    val contentNode = LanguageContent(nodeId, nodeId, initialContent, Some("nb"))
    val node = NodeToConvert(List(contentTitle), List(contentNode), copyright, List(tag), Seq(pageTitle), Seq(visualElement), Seq(relatedContents), Seq(), 0, 1)
    val expedtedResult = "<article>" + initialContent + "</article>"

    when(extractConvertStoreContent.processNode("4321")).thenReturn(Try(1: Long, ImportStatus(Seq(), Seq())))

    val (result, status) = service.toArticleInformation(node, ImportStatus(Seq(), Seq()))
    val strippedResult = result.article.head.article.replace("\n", "").replace(" ", "")

    strippedResult should equal (expedtedResult)
  }

  test("That content embedded in a node is converted") {
    val (nodeId, nodeId2) = ("1234", "4321")
    val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=inline==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val contentString2 = s"[contentbrowser ==nid=$nodeId2==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=inline==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val sampleOppgave1 = NodeGeneralContent(nodeId, nodeId, "Tittel", s"Innhold! $contentString2", "nb")
    val sampleOppgave2 = NodeGeneralContent(nodeId, nodeId2, "Tittel", "Enda mer innhold!", "nb")
    val initialContent = s"$contentString"
    val contentNode = LanguageContent(nodeId, nodeId, initialContent, Some("nb"))
    val node = NodeToConvert(List(contentTitle), List(contentNode), copyright, List(tag), Seq(pageTitle), Seq(visualElement), Seq(relatedContents), Seq(), 0, 1)

    when(extractService.getNodeType(nodeId)).thenReturn(Some("oppgave"))
    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleOppgave1))

    when(extractService.getNodeType(nodeId2)).thenReturn(Some("oppgave"))
    when(extractService.getNodeGeneralContent(nodeId2)).thenReturn(Seq(sampleOppgave2))

    val (result, status) = service.toArticleInformation(node, ImportStatus(Seq(), Seq()))
    result.article.head.article.replace("\n", "") should equal ("<article>  Innhold! Enda mer innhold! </article>")
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That the ingress is not added to the content") {
    val (nodeId, nodeId2) = ("1234", "4321")
    val ingressNodeBokmal = NodeIngress("1", "1", "Hvem er sterkest?", None, 1, Some("nn"))
    val contentNodeBokmal = LanguageContent(nodeId, nodeId, "<article>Nordavinden og sola kranglet en gang om hvem av dem som var den sterkeste</article>", Some("nb"))

    val ingressNodeNynorsk = NodeIngress("2", "2", "Kven er sterkast?", None, 1, Some("nn"))
    val contentNodeNynorsk = LanguageContent(nodeId2, nodeId, "<article>Nordavinden og sola krangla ein gong om kven av dei som var den sterkaste</article>", Some("nn"))

    val node = NodeToConvert(List(contentTitle), List(contentNodeBokmal, contentNodeNynorsk), copyright, List(tag), Seq(pageTitle), Seq(visualElement), Seq(), Seq(), 0, 1)
    val bokmalExpectedResult = "<article> Nordavinden og sola kranglet en gang om hvem av dem som var den sterkeste </article>"
    val nynorskExpectedResult = "<article> Nordavinden og sola krangla ein gong om kven av dei som var den sterkaste </article>"

    val (result, status) = service.toArticleInformation(node, ImportStatus(Seq(), Seq()))
    val bokmalStrippedResult = " +".r.replaceAllIn(result.article.head.article.replace("\n", ""), " ")
    val nynorskStrippedResult = " +".r.replaceAllIn(result.article.last.article.replace("\n", ""), " ")

    bokmalStrippedResult should equal (bokmalExpectedResult)
    nynorskStrippedResult should equal (nynorskExpectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }
}
