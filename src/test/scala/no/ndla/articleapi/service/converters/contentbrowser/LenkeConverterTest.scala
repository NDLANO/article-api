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
import org.mockito.Mockito._

class LenkeConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val linkUrl = "https://www.youtube.com/watch?v=1qN72LEQnaU"
  val nrkLinkUrl = "http://nrk.no/skole/klippdetalj?topic=urn%3Ax-mediadb%3A18745"
  val linkEmbedCode = s"""<$resourceHtmlEmbedTag data-id="1" data-resource="external" data-url="$linkUrl" />"""

  test("That LenkeConverter returns an embed code if insertion method is 'inline'") {
    val insertion = "inline"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"), 1)

    when(extractService.getNodeEmbedUrl(nodeId)).thenReturn(Some(linkUrl))

    val (result, requiredLibraries, errors) = LenkeConverter.convert(content, Seq())
    result should equal(linkEmbedCode)
    requiredLibraries.length should equal(0)
    errors.messages.length should equal(1)
  }

  test("That LenkeConverter returns an a-tag if insertion method is 'link'") {
    val insertion = "link"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = "<a href=\"https://www.youtube.com/watch?v=1qN72LEQnaU\" title=\"\"> </a>"

    when(extractService.getNodeEmbedUrl(nodeId)).thenReturn(Some(linkUrl))
    val (result, requiredLibraries, errors) = LenkeConverter.convert(content, Seq())
    result should equal(expectedResult)
    requiredLibraries.length should equal(0)
    errors.messages.length should equal(0)
  }

  test("That LenkeConverter defaults to 'link' if insertion method is not handled") {
    val insertion = "unhandledinsertion"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = "<a href=\"https://www.youtube.com/watch?v=1qN72LEQnaU\" title=\"\"> </a>"

    when(extractService.getNodeEmbedUrl(nodeId)).thenReturn(Some(linkUrl))
    val (result, requiredLibraries, errors) = LenkeConverter.convert(content, Seq())
    result should equal(expectedResult)
    requiredLibraries.length should equal(0)
    errors.messages.length should equal(1)
  }

  test("That LenkeConverter returns an a-tag if insertion method is 'lightbox_large'") {
    val insertion = "lightbox_large"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = "<a href=\"https://www.youtube.com/watch?v=1qN72LEQnaU\" title=\"\"> </a>"

    when(extractService.getNodeEmbedUrl(nodeId)).thenReturn(Some(linkUrl))
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

    when(extractService.getNodeEmbedUrl(nodeId)).thenReturn(Some(linkUrl))
    val (result, requiredLibraries, errors) = LenkeConverter.convert(content, Seq())

    result should equal(expectedResult)
    requiredLibraries.length should equal(0)
    errors.messages.length should equal(1)
  }

  test("That LenkeConverter returns inline content with nrk video id") {
    val insertion = "inline"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"), 1)
    val nrkVideoId = "94605"
    val nrkScriptUrl = "https://www.nrk.no/serum/latest/js/video_embed.js"
    val expectedResult = s"""<$resourceHtmlEmbedTag data-id="1" data-nrk-video-id="$nrkVideoId" data-resource="external" data-url="$nrkLinkUrl" />"""

    when(extractService.getNodeEmbedUrl(nodeId)).thenReturn(Some(nrkLinkUrl))
    when(extractService.getNodeEmbedCode(nodeId)).thenReturn(Some(s"""<div class="nrk-video" data-nrk-id="$nrkVideoId"></div><script src="$nrkScriptUrl"></script>"""))
    val (result, requiredLibraries, errors) = LenkeConverter.convert(content, Seq())

    result should equal(expectedResult)
    errors.messages.length should equal(1)
    requiredLibraries.length should equal(1)
    requiredLibraries.head.url should equal(nrkScriptUrl)
  }
}
