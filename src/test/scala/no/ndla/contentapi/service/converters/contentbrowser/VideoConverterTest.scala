package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.TestEnvironment
import no.ndla.learningpathapi.UnitSuite

class VideoConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"

  test("That VideoConverter converts a ContentBrowser to html code") {
    val content = ContentBrowser(contentString)
    val expectedResult = s"""<div style="display: block; position: relative; max-width: 100%;">
                           | <div style="padding-top: 56.25%;">
                           | <video
                           | data-video-id="ref:$nodeId"
                           | data-account="4806596774001"
                           | data-player="BkLm8fT"
                           | data-embed="default"
                           | class="video-js"
                           | controls
                           | style="width: 100%; height: 100%; position: absolute; top: 0px; bottom: 0px; right: 0px; left: 0px;">
                           | </video>
                           | </div></div>""".stripMargin.replace("\n", "")
    val (result, requiredLibraries, messages) = VideoConverter.convert(content)
    val strippedResult = " +".r.replaceAllIn(result, " ")
    strippedResult.replace("\n", "") should equal(expectedResult)
    requiredLibraries.length should equal(1)
  }
}
