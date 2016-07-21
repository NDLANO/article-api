package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.TestEnvironment
import no.ndla.contentapi.UnitSuite
import no.ndla.contentapi.ContentApiProperties.{NDLABrightcoveAccountId, NDLABrightcovePlayerId}

class VideoConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"

  test("That VideoConverter converts a ContentBrowser to html code") {
    val content = ContentBrowser(contentString, Some("nb"), 1)
    val expectedResult = s"""<figure data-resource="brightcove" data-id="1" data-videoid="ref:${content.get("nid")}" data-account="$NDLABrightcoveAccountId" data-player="$NDLABrightcovePlayerId"></figure>"""

    val (result, requiredLibraries, messages) = VideoConverter.convert(content, Seq())
    val strippedResult = " +".r.replaceAllIn(result, " ")
    strippedResult.replace("\n", "") should equal(expectedResult)
    requiredLibraries.length should equal(1)
  }
}
