package no.ndla.contentapi.service

import no.ndla.contentapi.TestEnvironment
import no.ndla.contentapi.integration.{LanguageContent, NodeGeneralContent, NodeIngress, NodeToConvert}
import no.ndla.contentapi.model._
import no.ndla.contentapi.UnitSuite
import org.mockito.Mockito._

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  val service = new ConverterService
  val contentTitle = ContentTitle("", Some(""))
  val license = License("licence", "description", Some("http://"))
  val author = Author("forfatter", "Henrik")
  val copyright = Copyright(license, "", List(author))
  val tag = ContentTag("asdf", Some("nb"))
  val requiredLibrary = RequiredLibrary("", "", "")

  test("That the document is wrapped in an article tag") {
    val nodeId = "1"
    val initialContent = "<h1>Heading</h1>"
    val contentNode = LanguageContent(nodeId, nodeId, initialContent, Some("nb"))
    val node = NodeToConvert(List(contentTitle), List(contentNode), copyright, List(tag))
    val expedtedResult = "<article>" + initialContent + "</article>"

    when(extractService.getNodeIngress(nodeId)).thenReturn(None)

    service.convertNode(node)._1.content(0).content.replace("\n", "").replace(" ", "") should equal (expedtedResult)
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
    val node = NodeToConvert(List(contentTitle), List(contentNode), copyright, List(tag))

    when(extractService.getNodeIngress(nodeId)).thenReturn(None)
    when(extractService.getNodeType(nodeId)).thenReturn(Some("oppgave"))
    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleOppgave1))

    when(extractService.getNodeIngress(nodeId2)).thenReturn(None)
    when(extractService.getNodeType(nodeId2)).thenReturn(Some("oppgave"))
    when(extractService.getNodeGeneralContent(nodeId2)).thenReturn(Seq(sampleOppgave2))

    val (result, status) = service.convertNode(node)
    result.content.head.content.replace("\n", "") should equal ("<article>  Innhold! Enda mer innhold! </article>")
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That the correct language ingress is added to the content") {
    val (nodeId, nodeId2) = ("1234", "4321")
    val contentNodeBokmal = LanguageContent(nodeId, nodeId, "<article>Nordavinden og sola kranglet en gang om hvem av dem som var den sterkeste</article>", Some("nb"))
    val contentNodeNynorsk = LanguageContent(nodeId2, nodeId, "<article>Nordavinden og sola krangla ein gong om kven av dei som var den sterkaste</article>", Some("nn"))
    val node = NodeToConvert(List(contentTitle), List(contentNodeBokmal, contentNodeNynorsk), copyright, List(tag))
    val ingressNodeBokmal = NodeIngress(nodeId, "Hvem er sterkest?", None, 1)
    val ingressNodeNynorsk = NodeIngress(nodeId2, "Kven er sterkast?", None, 1)
    val bokmalExpectedResult = "<article> <section> Hvem er sterkest? </section> Nordavinden og sola kranglet en gang om hvem av dem som var den sterkeste </article>"
    val nynorskExpectedResult = "<article> <section> Kven er sterkast? </section> Nordavinden og sola krangla ein gong om kven av dei som var den sterkaste </article>"

    when(extractService.getNodeIngress(nodeId)).thenReturn(Some(ingressNodeBokmal))
    when(extractService.getNodeIngress(nodeId2)).thenReturn(Some(ingressNodeNynorsk))

    val (result, status) = service.convertNode(node)
    val bokmalStrippedResult = " +".r.replaceAllIn(result.content.head.content.replace("\n", ""), " ")
    val nynorskStrippedResult = " +".r.replaceAllIn(result.content.last.content.replace("\n", ""), " ")

    bokmalStrippedResult should equal (bokmalExpectedResult)
    nynorskStrippedResult should equal (nynorskExpectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }
}
