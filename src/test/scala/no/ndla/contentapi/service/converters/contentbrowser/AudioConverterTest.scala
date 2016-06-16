package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.integration.AudioMeta
import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import no.ndla.contentapi.ContentApiProperties.amazonUrlPrefix
import org.mockito.Mockito._

class AudioConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val content = ContentBrowser(contentString)

  test("That AudioConverter returns a HTML audio player with the mp3 uploaded to an S3 bucket") {
    val audio = AudioMeta(nodeId, "title", "1:00", "mp3", "audio/mp3", "1024", "goat.mp3", "/audio/goat.mp3")
    val expectedResult = s"""<figure> <figcaption>title</figcaption> <audio src=\"$amazonUrlPrefix/$nodeId/goat.mp3\" preload=\"auto\" controls> Your browser does not support the <code>video</code> element. </audio> </figure> """

    when(extractService.getAudioMeta(nodeId)).thenReturn(Some(audio))
    when(storageService.uploadAudiofromUrl(nodeId, audio)).thenReturn(s"$nodeId/goat.mp3")

    val (result, requiredLibraries, status) = AudioConverter.convert(content)
    val strippedResult = " +".r.replaceAllIn(result, " ")

    strippedResult.replace("\n", "") should equal (expectedResult)
  }

  test("That AudioConverter returns an error if the audio was not found") {
    val expectedResult = s"{Error: Failed to retrieve audio metadata for node $nodeId}"

    when(extractService.getAudioMeta(nodeId)).thenReturn(None)

    val (result, requiredLibraries, status) = AudioConverter.convert(content)
    result.replace("\n", "") should equal (expectedResult)
    status.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }
}
