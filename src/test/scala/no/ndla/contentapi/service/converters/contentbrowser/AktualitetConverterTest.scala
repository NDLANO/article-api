package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.ContentApiProperties._
import no.ndla.contentapi.integration.ContentAktualitet
import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._


class AktualitetConverterTest extends UnitSuite with TestEnvironment {
  val (nodeId, nodeId2) = ("1234", "4321")
  val insertion = "inline"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val sampleAktualitet1 = ContentAktualitet(nodeId, nodeId, "Tittel", "Innhold", "nb")
  val sampleAktualitet2 = ContentAktualitet(nodeId, nodeId2, "Tittel", "Innhald", "nn")

  test("That AktualitetConverter returns the contents of a aktualitet according to language") {
    val content = ContentBrowser(contentString, Some("nb"))

    when(extractService.getNodeAktualitet(nodeId)).thenReturn(Seq(sampleAktualitet1, sampleAktualitet2))
    val (result, requiredLibraries, status) = AktualitetConverter.convert(content)

    result should equal (sampleAktualitet1.aktualitet)
    status.isEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That AktualitetConverter returns an error when the aktualitet is not found") {
    val content = ContentBrowser(contentString, Some("nb"))

    when(extractService.getNodeAktualitet(nodeId)).thenReturn(Seq())
    val (result, requiredLibraries, status) = AktualitetConverter.convert(content)

    result should equal (s"{Import error: Failed to retrieve 'aktualitet' ($nodeId)}")
    status.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That AktualitetConverter inserts the content if insertion mode is 'collapsed_body'") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=collapsed_body==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"<details><summary>Tittel</summary>${sampleAktualitet1.aktualitet}</details>"

    when(extractService.getNodeAktualitet(nodeId)).thenReturn(Seq(sampleAktualitet1, sampleAktualitet2))
    val (result, requiredLibraries, status) = AktualitetConverter.convert(content)
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.isEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }


  test("That AktualitetConverter inserts the content if insertion mode is 'link'") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=link==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"""<a href="$ndlaBaseHost/node/$nodeId">Tittel</a>"""

    when(extractService.getNodeAktualitet(nodeId)).thenReturn(Seq(sampleAktualitet1, sampleAktualitet2))
    val (result, requiredLibraries, status) = AktualitetConverter.convert(content)
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }

  test("That AktualitetConverter inserts the content if insertion mode is 'lightbox_large'") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=lightbox_large==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"""<a href="$ndlaBaseHost/node/$nodeId">Tittel</a>"""

    when(extractService.getNodeAktualitet(nodeId)).thenReturn(Seq(sampleAktualitet1, sampleAktualitet2))
    val (result, requiredLibraries, status) = AktualitetConverter.convert(content)
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }


  test("That AktualitetConverter defaults to 'link' if the insertion method is unknown") {
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=lightbox_large==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"""<a href="$ndlaBaseHost/node/$nodeId">Tittel</a>"""

    when(extractService.getNodeAktualitet(nodeId)).thenReturn(Seq(sampleAktualitet1, sampleAktualitet2))
    val (result, requiredLibraries, status) = AktualitetConverter.convert(content)
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }
}
