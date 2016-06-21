package no.ndla.contentapi.service

import no.ndla.contentapi.TestEnvironment
import no.ndla.contentapi.integration.ContentOppgave
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
    val initialContent = "<h1>Heading</h1>"
    val node = ContentInformation("1", List(contentTitle), List(Content(initialContent, Some("nb"))), copyright, List(tag), List(requiredLibrary))
    val expedtedResult = "<article>" + initialContent + "</article>"

    service.convertNode(node)._1.content(0).content.replace("\n", "").replace(" ", "") should equal (expedtedResult)
  }

  test("That content embedded in a node is converted") {
    val (nodeId, nodeId2) = ("1234", "4321")
    val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val contentString2 = s"[contentbrowser ==nid=$nodeId2==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val sampleOppgave1 = ContentOppgave(nodeId, nodeId, "Tittel", s"Innhold! $contentString2", "nb")
    val sampleOppgave2 = ContentOppgave(nodeId, nodeId2, "Tittel", "Enda mer innhold!", "nb")
    val initialContent = s"$contentString"
    val node = ContentInformation(nodeId, List(contentTitle), List(Content(initialContent, Some("nb"))), copyright, List(tag), List(requiredLibrary))

    when(extractService.getNodeType(nodeId)).thenReturn(Some("oppgave"))
    when(extractService.getNodeOppgave(nodeId)).thenReturn(Seq(sampleOppgave1))

    when(extractService.getNodeType(nodeId2)).thenReturn(Some("oppgave"))
    when(extractService.getNodeOppgave(nodeId2)).thenReturn(Seq(sampleOppgave2))

    val (result, status) = service.convertNode(node)
    result.content(0).content.replace("\n", "") should equal ("<article>  Innhold! Enda mer innhold! </article>")
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }
}
