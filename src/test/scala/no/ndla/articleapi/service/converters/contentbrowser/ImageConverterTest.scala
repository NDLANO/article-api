/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.model.domain.{Author, ImportStatus}
import no.ndla.articleapi.integration._
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import org.mockito.Mockito._

import scala.util.Success

class ImageConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val caption = "sample image caption"
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text=$caption==text_align===css_class=contentbrowser contentbrowser]"
  val contentStringWithLeftMargin = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text=$caption==text_align===css_class=contentbrowser contentbrowser_margin_left contentbrowser]"
  val contentStringEmptyCaption = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text===text_align===css_class=contentbrowser contentbrowser]"
  val content = ContentBrowser(contentString, "nb")
  val license = ImageLicense("licence", "description", Some("http://"))
  val author = Author("forfatter", "Henrik")
  val copyright = ImageCopyright(license, "", List(author))

  test("That a contentbrowser string of type 'image' returns an HTML img-tag with path to image") {
    val image = ImageMetaInformation("1234", List(ImageTitle("", Some("nb"))), List(ImageAltText("", Some("nb"))), "full.jpg", 1024, "", copyright, List(ImageTag(List(""), Some(""))))
    val expectedResult = s"""<$resourceHtmlEmbedTag data-align="" data-alt="$altText" data-caption="$caption" data-resource="image" data-resource_id="1234" data-size="fullbredde" />"""

    when(imageApiClient.importOrGetMetaByExternId(nodeId)).thenReturn(Some(image))

    val Success((result, requiredLibraries, errors)) = ImageConverter.convert(content, ImportStatus.empty)
    result should equal (expectedResult)
    errors.messages.length should equal(0)
    requiredLibraries.length should equal(0)
  }

  test("That the the data-captions attribute is empty if no captions exist") {
    val image = ImageMetaInformation("1234", List(ImageTitle("", Some("nb"))), List(ImageAltText("", Some("nb"))), "full.jpg", 1024, "", copyright, List(ImageTag(List(""), Some(""))))
    val expectedResult = s"""<$resourceHtmlEmbedTag data-align="" data-alt="$altText" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" />"""

    when(imageApiClient.importOrGetMetaByExternId(nodeId)).thenReturn(Some(image))
    val Success((result, requiredLibraries, errors)) = ImageConverter.convert(ContentBrowser(contentStringEmptyCaption, "nb"), ImportStatus.empty)

    result should equal (expectedResult)
    errors.messages.length should equal(0)
    requiredLibraries.length should equal(0)
  }

  test("That a contentbrowser string of type 'image' returns a Failure if image is inexistant") {
    val expectedResult = s"""<img src='stock.jpeg' alt='The image with id $nodeId was not not found' />"""

    when(imageApiClient.importOrGetMetaByExternId(nodeId)).thenReturn(None)
    ImageConverter.convert(content, ImportStatus.empty).isFailure should be (true)
  }

  test("That a the html tag contains an alignment attribute with the correct value") {
    val image = ImageMetaInformation("1234", List(ImageTitle("", Some("nb"))), List(ImageAltText("", Some("nb"))), "full.jpg", 1024, "", copyright, List(ImageTag(List(""), Some(""))))
    val expectedResult = s"""<$resourceHtmlEmbedTag data-align="right" data-alt="$altText" data-caption="$caption" data-resource="image" data-resource_id="1234" data-size="fullbredde" />"""

    when(imageApiClient.importOrGetMetaByExternId(nodeId)).thenReturn(Some(image))
    val Success((result, requiredLibraries, errors)) = ImageConverter.convert(ContentBrowser(contentStringWithLeftMargin, "nb"), ImportStatus.empty)

    result should equal (expectedResult)
    errors.messages.length should equal(0)
    requiredLibraries.length should equal(0)
  }
}
