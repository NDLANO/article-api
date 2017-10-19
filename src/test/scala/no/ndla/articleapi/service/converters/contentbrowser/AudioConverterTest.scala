/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.model.domain.ImportStatus
import org.mockito.Mockito._

import scala.util.{Failure, Success}

class AudioConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val content = ContentBrowser(contentString, "nb")

  test("That AudioConverter returns a embed resource string if the audio was imported") {
    val audioId: Long = 123
    val expectedResult = s"""<$resourceHtmlEmbedTag data-resource="audio" data-resource_id="123" />"""

    when(audioApiClient.getOrImportAudio(nodeId)).thenReturn(Success(audioId))

    val Success((result, requiredLibraries, status)) = AudioConverter.convert(content, ImportStatus.empty)
    val strippedResult = " +".r.replaceAllIn(result.replace("\n", ""), " ")

    status.messages.isEmpty should be (true)
    requiredLibraries.isEmpty should be(true)
    strippedResult should equal (expectedResult)
  }

  test("That AudioConverter returns a Failure if the audio was not found") {
    when(audioApiClient.getOrImportAudio(nodeId)).thenReturn(Failure(new RuntimeException("error")))
    AudioConverter.convert(content, ImportStatus.empty).isFailure should be (true)
  }
}
