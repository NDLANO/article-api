package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import no.ndla.contentapi.integration.ContentOppgave
import no.ndla.contentapi.ContentApiProperties.ndlaBaseHost
import org.mockito.Mockito._

class OppgaveConverterTest extends UnitSuite with TestEnvironment {
  val (nodeId, nodeId2) = ("1234", "4321")
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=inline==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val sampleOppgave1 = ContentOppgave(nodeId, nodeId, "Tittel", "Innhold", "nb")
  val sampleOppgave2 = ContentOppgave(nodeId, nodeId2, "Tittel", "Innhald", "nn")

  test("That OppgaveConverter returns the contents of an oppgave according to language") {
    val content = ContentBrowser(contentString, Some("nb"))

    when(extractService.getNodeOppgave(nodeId)).thenReturn(Seq(sampleOppgave1, sampleOppgave2))
    val (result, requiredLibraries, status) = OppgaveConverter.convert(content)

    result should equal (sampleOppgave1.content)
    status.isEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That OppgaveConverter returns an error when the oppgave is not found") {
    val content = ContentBrowser(contentString, Some("nb"))

    when(extractService.getNodeOppgave(nodeId)).thenReturn(Seq())
    val (result, requiredLibraries, status) = OppgaveConverter.convert(content)

    result should equal (s"{Import error: Failed to retrieve 'oppgave' with language 'nb' ($nodeId)}")
    status.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That OppgaveConverter inserts the content if insertion mode is 'collapsed_body'") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=collapsed_body==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"<details><summary>Tittel</summary>${sampleOppgave1.content}</details>"

    when(extractService.getNodeOppgave(nodeId)).thenReturn(Seq(sampleOppgave1, sampleOppgave2))
    val (result, requiredLibraries, status) = OppgaveConverter.convert(content)
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.isEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That OppgaveConverter inserts the content if insertion mode is 'link'") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=link==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"""<a href="$ndlaBaseHost/node/$nodeId">Tittel</a>"""

    when(extractService.getNodeOppgave(nodeId)).thenReturn(Seq(sampleOppgave1, sampleOppgave2))
    val (result, requiredLibraries, status) = OppgaveConverter.convert(content)
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That OppgaveConverter inserts the content if insertion mode is 'lightbox_large'") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=lightbox_large==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"""<a href="$ndlaBaseHost/node/$nodeId">Tittel</a>"""

    when(extractService.getNodeOppgave(nodeId)).thenReturn(Seq(sampleOppgave1, sampleOppgave2))
    val (result, requiredLibraries, status) = OppgaveConverter.convert(content)
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }


  test("That OppgaveConverter defaults to 'link' if the insertion method is unknown") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=lightbox_large==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"""<a href="$ndlaBaseHost/node/$nodeId">Tittel</a>"""

    when(extractService.getNodeOppgave(nodeId)).thenReturn(Seq(sampleOppgave1, sampleOppgave2))
    val (result, requiredLibraries, status) = OppgaveConverter.convert(content)
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

}
