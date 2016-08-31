/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._

class LenkeConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val linkUrl = "https://www.youtube.com/watch?v=1qN72LEQnaU"
  val linkEmbedCode = s"""<figure data-resource="external" data-id="1" data-url="$linkUrl"></figure>"""

  test("That LenkeConverter returns an embed code if insertion method is 'inline'") {
    val insertion = "inline"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"), 1)

    when(extractService.getNodeEmbedData(nodeId)).thenReturn(Some(linkUrl))

    val (result, requiredLibraries, errors) = LenkeConverter.convert(content, Seq())
    result should equal(linkEmbedCode)
    requiredLibraries.length should equal(0)
    errors.messages.length should equal(0)
  }

  test("That LenkeConverter returns an a-tag if insertion method is 'link'") {
    val insertion = "link"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = "<a href=\"https://www.youtube.com/watch?v=1qN72LEQnaU\" title=\" \"> </a>"

    when(extractService.getNodeEmbedData(nodeId)).thenReturn(Some(linkUrl))
    val (result, requiredLibraries, errors) = LenkeConverter.convert(content, Seq())
    result should equal(expectedResult)
    requiredLibraries.length should equal(0)
    errors.messages.length should equal(0)
  }

  test("That LenkeConverter returns an a-tag if insertion method is 'lightbox_large'") {
    val insertion = "lightbox_large"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = "<a href=\"https://www.youtube.com/watch?v=1qN72LEQnaU\" title=\" \"> </a>"

    when(extractService.getNodeEmbedData(nodeId)).thenReturn(Some(linkUrl))
    val (result, requiredLibraries, errors) = LenkeConverter.convert(content, Seq())
    result should equal(expectedResult)
    requiredLibraries.length should equal(0)
    errors.messages.length should equal(0)
  }

  test("That LenkeConverter returns a collapsed embed code if insertion method is 'collapsed_body'") {
    val insertion = "collapsed_body"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"), 1)
    val expectedResult = s"<details><summary>${content.get("link_text")}</summary>$linkEmbedCode</details>"

    when(extractService.getNodeEmbedData(nodeId)).thenReturn(Some(linkUrl))
    val (result, requiredLibraries, errors) = LenkeConverter.convert(content, Seq())

    result should equal(expectedResult)
    requiredLibraries.length should equal(0)
    errors.messages.length should equal(0)
  }
}