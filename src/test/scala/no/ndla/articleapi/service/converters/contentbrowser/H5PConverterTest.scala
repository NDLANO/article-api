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

import scala.util.Success

class H5PConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val content = ContentBrowser(contentString, Some("nb"))

  test("That contentbrowser strings of type 'h5p_content' returns an iframe") {
    val expectedResult = s"""<$resourceHtmlEmbedTag data-resource="h5p" data-url="//ndla.no/h5p/embed/1234" />"""
    val Success((result, requiredLibraries, errors)) = H5PConverter.convert(content, Seq())

    result should equal(expectedResult)
    errors.messages.length should equal(0)
    requiredLibraries.length should be > 0
  }
}
