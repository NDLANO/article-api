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
import no.ndla.articleapi.integration.MigrationEmbedMeta
import no.ndla.articleapi.service.converters.ResourceType
import org.mockito.Mockito._

import scala.util.Success

class LenkeConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val linkUrl = "https://www.youtube.com/watch?v=1qN72LEQnaU"
  val nrkVideoId = "94605"
  val nrkScriptUrl = "https://www.nrk.no/serum/latest/js/video_embed.js"
  val nrkEmbedScript = s"""<div class="nrk-video" data-nrk-id="$nrkVideoId"></div><script src="$nrkScriptUrl"></script>"""
  val nrkLinkUrl = "http://nrk.no/skole/klippdetalj?topic=urn%3Ax-mediadb%3A18745"
  val linkEmbedCode = s"""<$resourceHtmlEmbedTag data-resource="external" data-url="$linkUrl" />"""

  override def beforeEach = {
    when(extractService.getNodeEmbedMeta(nodeId)).thenReturn(Success(MigrationEmbedMeta(Some(linkUrl), None)))
  }

  test("That LenkeConverter returns an embed code if insertion method is 'inline'") {
    val insertion = "inline"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, "nb")

    val Success((result, requiredLibraries, errors)) = LenkeConverter.convert(content, Seq())
    result should equal(linkEmbedCode)
    requiredLibraries.length should equal(0)
    errors.messages.length should equal(1)
  }

  test("That LenkeConverter returns an a-tag if insertion method is 'link'") {
    val insertion = "link"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, "nb")
    val expectedResult = " <a href=\"https://www.youtube.com/watch?v=1qN72LEQnaU\" title=\"\"> </a>"

    val Success((result, requiredLibraries, errors)) = LenkeConverter.convert(content, Seq())
    result should equal(expectedResult)
    requiredLibraries.length should equal(0)
    errors.messages.length should equal(0)
  }

  test("That LenkeConverter defaults to 'link' if insertion method is not handled") {
    val insertion = "unhandledinsertion"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, "nb")
    val expectedResult = " <a href=\"https://www.youtube.com/watch?v=1qN72LEQnaU\" title=\"\"> </a>"

    val Success((result, requiredLibraries, errors)) = LenkeConverter.convert(content, Seq())
    result should equal(expectedResult)
    requiredLibraries.length should equal(0)
    errors.messages.length should equal(1)
  }

  test("That LenkeConverter returns an a-tag if insertion method is 'lightbox_large'") {
    val insertion = "lightbox_large"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, "nb")
    val expectedResult = " <a href=\"https://www.youtube.com/watch?v=1qN72LEQnaU\" title=\"\"> </a>"

    val Success((result, requiredLibraries, errors)) = LenkeConverter.convert(content, Seq())
    result should equal(expectedResult)
    requiredLibraries.length should equal(0)
    errors.messages.length should equal(0)
  }

  test("That LenkeConverter returns a collapsed embed code if insertion method is 'collapsed_body'") {
    val insertion = "collapsed_body"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, "nb")
    val expectedResult = s"<details><summary>${content.get("link_text")}</summary>$linkEmbedCode</details>"

    val Success((result, requiredLibraries, errors)) = LenkeConverter.convert(content, Seq())

    result should equal(expectedResult)
    requiredLibraries.length should equal(0)
    errors.messages.length should equal(1)
  }

  test("That LenkeConverter returns inline content with nrk video id") {
    val insertion = "inline"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, "nb")
    val expectedResult = s"""<$resourceHtmlEmbedTag data-nrk-video-id="$nrkVideoId" data-resource="nrk" data-url="$nrkLinkUrl" />"""

    when(extractService.getNodeEmbedMeta(nodeId)).thenReturn(Success(MigrationEmbedMeta(Some(nrkLinkUrl), Some(nrkEmbedScript))))
    val Success((result, requiredLibraries, errors)) = LenkeConverter.convert(content, Seq())

    result should equal(expectedResult)
    errors.messages.length should equal(1)
    requiredLibraries.length should equal(1)
    requiredLibraries.head.url should equal(nrkScriptUrl.replace("https:", ""))
  }

  test("That LenkeConverter returns a prezi embed for prezi resources") {
    val preziUrl = "http://prezi.com/123123123"
    val preziSrc = "https://prezi.com/embed/123123123&autoplay=0"
    val preziEmbedCode = s"""<iframe id="iframe_container" frameborder="0" webkitallowfullscreen="" mozallowfullscreen="" allowfullscreen="" width="620" height="451" src="$preziSrc"></iframe>"""

    val insertion = "inline"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, "nb")
    val expectedResult = s"""<$resourceHtmlEmbedTag data-height="451" data-resource="${ResourceType.Prezi}" data-url="$preziSrc" data-width="620" />"""

    when(extractService.getNodeEmbedMeta(nodeId)).thenReturn(Success(MigrationEmbedMeta(Some(preziUrl), Some(preziEmbedCode))))
    val Success((result, _, errors)) = LenkeConverter.convert(content, Seq())

    result should equal(expectedResult)
    errors.messages.length should equal(1)
  }

  test("That LenkeConverter returns a commoncraft embed for commoncraft resources") {
    val CcraftUrl = "http://www.commoncraft.com/123123123"
    val CcraftSrc = "http://www.commoncraft.com/embed/db233ba&autoplay=0"
    val CcraftEmbedCode = s"""<iframe id="cc-embed" frameborder="0" width="620" height="451" src="$CcraftSrc" scrolling="false"></iframe>"""

    val insertion = "inline"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, "nb")
    val expectedResult = s"""<$resourceHtmlEmbedTag data-height="451" data-resource="${ResourceType.Commoncraft}" data-url="$CcraftSrc" data-width="620" />"""

    when(extractService.getNodeEmbedMeta(nodeId)).thenReturn(Success(MigrationEmbedMeta(Some(CcraftUrl), Some(CcraftEmbedCode))))
    val Success((result, _, errors)) = LenkeConverter.convert(content, Seq())

    result should equal(expectedResult)
    errors.messages.length should equal(1)
  }
}
