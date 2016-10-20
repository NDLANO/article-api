/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.model.domain.{Author, Copyright, License}
import no.ndla.articleapi.integration._
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.articleapi.service._
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import org.mockito.Mockito._

class ImageConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val caption = "sample image caption"
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text=$caption==text_align===css_class=contentbrowser contentbrowser]"
  val contentStringWithLeftMargin = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text=$caption==text_align===css_class=contentbrowser contentbrowser_margin_left contentbrowser]"
  val contentStringEmptyCaption = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text===text_align===css_class=contentbrowser contentbrowser]"
  val content = ContentBrowser(contentString, Some("nb"), 1)
  val license = License("licence", "description", Some("http://"))
  val author = Author("forfatter", "Henrik")
  val copyright = Copyright(license, "", List(author))

  test("That a contentbrowser string of type 'image' returns an HTML img-tag with path to image") {
    val (small, full) = (Image("small.jpg", 1024, ""), Image("full.jpg", 1024, ""))
    val imageVariants = ImageVariants(Some(small), Some(full))
    val image = ImageMetaInformation("1234", List(ImageTitle("", Some("nb"))), List(ImageAltText("", Some("nb"))), imageVariants, copyright, List(ImageTag(List(""), Some(""))))
    val expectedResult = s"""<$resourceHtmlEmbedTag data-align="" data-alt="$altText" data-caption="$caption" data-id="1" data-resource="image" data-size="fullbredde" data-url="http://localhost/images/$nodeId" />"""

    when(imageApiClient.importOrGetMetaByExternId(nodeId)).thenReturn(Some(image))

    val (result, requiredLibraries, errors) = ImageConverter.convert(content, Seq())
    result should equal (expectedResult)
    errors.messages.length should equal(0)
    requiredLibraries.length should equal(0)
  }

  test("That the the data-captions attribute is empty if no captions exist") {
    val (small, full) = (Image("small.jpg", 1024, ""), Image("full.jpg", 1024, ""))
    val imageVariants = ImageVariants(Some(small), Some(full))
    val image = ImageMetaInformation("1234", List(ImageTitle("", Some("nb"))), List(ImageAltText("", Some("nb"))), imageVariants, copyright, List(ImageTag(List(""), Some(""))))
    val expectedResult = s"""<$resourceHtmlEmbedTag data-align="" data-alt="$altText" data-caption="" data-id="1" data-resource="image" data-size="fullbredde" data-url="http://localhost/images/$nodeId" />"""

    when(imageApiClient.importOrGetMetaByExternId(nodeId)).thenReturn(Some(image))
    val (result, requiredLibraries, errors) = ImageConverter.convert(ContentBrowser(contentStringEmptyCaption, Some("nb"), 1), Seq())

    result should equal (expectedResult)
    errors.messages.length should equal(0)
    requiredLibraries.length should equal(0)
  }

  test("That a contentbrowser string of type 'image' returns an HTML img-tag with a stock image if image is inexistant") {
    val expectedResult = s"""<img src='stock.jpeg' alt='The image with id $nodeId was not not found' />"""

    when(imageApiClient.importOrGetMetaByExternId(nodeId)).thenReturn(None)

    val (result, requiredLibraries, errors) = ImageConverter.convert(content, Seq())

    result should equal (expectedResult)
    errors.messages.length should be > 0
    requiredLibraries.length should equal(0)
  }

  test("That a the html tag contains an alignment attribute with the correct value") {
    val (small, full) = (Image("small.jpg", 1024, ""), Image("full.jpg", 1024, ""))
    val imageVariants = ImageVariants(Some(small), Some(full))
    val image = ImageMetaInformation("1234", List(ImageTitle("", Some("nb"))), List(ImageAltText("", Some("nb"))), imageVariants, copyright, List(ImageTag(List(""), Some(""))))
    val expectedResult = s"""<$resourceHtmlEmbedTag data-align="right" data-alt="$altText" data-caption="$caption" data-id="1" data-resource="image" data-size="fullbredde" data-url="http://localhost/images/$nodeId" />"""

    when(imageApiClient.importOrGetMetaByExternId(nodeId)).thenReturn(Some(image))
    val (result, requiredLibraries, errors) = ImageConverter.convert(ContentBrowser(contentStringWithLeftMargin, Some("nb"), 1), Seq())

    result should equal (expectedResult)
    errors.messages.length should equal(0)
    requiredLibraries.length should equal(0)
  }
}
