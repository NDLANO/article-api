package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.articleapi.ContentApiProperties.amazonUrlPrefix
import no.ndla.articleapi.model.ContentFilMeta._
import no.ndla.articleapi.model.ContentFilMeta
import org.mockito.Mockito._

class AudioConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val content = ContentBrowser(contentString, Some("nb"))

  test("That AudioConverter returns a HTML audio player with the mp3 uploaded to an S3 bucket") {
    val audio = ContentFilMeta(nodeId, "0", "title", "goat.mp3", "http://audio/goat.mp3", "audio/mp3", "1024")
    val expectedResult = s"""<figure> <figcaption>title</figcaption> <audio src=\"$amazonUrlPrefix/$nodeId/goat.mp3\" preload=\"auto\" controls> Your browser does not support the <code>audio</code> element. </audio> </figure> """

    when(extractService.getAudioMeta(nodeId)).thenReturn(Some(audio))
    when(storageService.uploadFileFromUrl(nodeId, audio)).thenReturn(Some(s"$nodeId/goat.mp3"))

    val (result, requiredLibraries, status) = AudioConverter.convert(content, Seq())
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    strippedResult.replace("\n", "") should equal (expectedResult)
  }

  test("That AudioConverter returns an error if the audio was not found") {
    val expectedResult = s"{Error: Failed to retrieve audio metadata for node $nodeId}"

    when(extractService.getAudioMeta(nodeId)).thenReturn(None)

    val (result, requiredLibraries, status) = AudioConverter.convert(content, Seq())
    result.replace("\n", "") should equal (expectedResult)
    status.messages.nonEmpty should equal (true)
    requiredLibraries.isEmpty should equal (true)
  }
}
