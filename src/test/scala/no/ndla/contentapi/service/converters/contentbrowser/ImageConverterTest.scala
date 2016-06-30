package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import no.ndla.contentapi.model.{Author, Copyright, License}
import no.ndla.contentapi.service._
import org.mockito.Mockito._

class ImageConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val content = ContentBrowser(contentString, Some("nb"))
  val license = License("licence", "description", Some("http://"))
  val author = Author("forfatter", "Henrik")
  val copyright = Copyright(license, "", List(author))

  test("That a contentbrowser string of type 'image' returns an HTML img-tag with path to image") {
    val (small, full) = (Image("small.jpg", 1024, ""), Image("full.jpg", 1024, ""))
    val imageVariants = ImageVariants(Some(small), Some(full))
    val image = ImageMetaInformation("1234", List(ImageTitle("", Some("nb"))), List(ImageAltText("", Some("nb"))), imageVariants, copyright, List(ImageTag("", Some(""))))
    val expectedResult = s"""<img src=\"/images/full.jpg\" alt=\"$altText\" />"""

    when(imageApiService.getMetaByExternId(nodeId)).thenReturn(Some(image))

    val (result, requiredLibraries, errors) = ImageConverter.convert(content)
    result should equal (expectedResult)
    errors.length should equal(0)
    requiredLibraries.length should equal(0)
  }

  test("That a contentbrowser string of type 'image' imports the image if it does not exist") {
    val (small, full) = (Image("small.jpg", 1024, ""), Image("full.jpg", 1024, ""))
    val imageVariants = ImageVariants(Some(small), Some(full))
    val image = ImageMetaInformation("1234", List(ImageTitle("", Some("nb"))), List(ImageAltText("", Some("nb"))), imageVariants, copyright, List(ImageTag("", Some(""))))
    val expectedResult = s"""<img src=\"/images/full.jpg\" alt=\"$altText\" />"""

    when(imageApiService.getMetaByExternId(nodeId)).thenReturn(None)
    when(imageApiService.importImage(nodeId)).thenReturn(Some(image))

    val (result, requiredLibraries, errors) = ImageConverter.convert(content)
    result should equal (expectedResult)
    errors.length should equal(0)
    requiredLibraries.length should equal(0)
  }

  test("That a contentbrowser string of type 'image' returns an HTML img-tag with a stock image if image is inexistant") {
    val expectedResult = s"""<img src='stock.jpeg' alt='The image with id $nodeId was not not found' />"""

    when(imageApiService.getMetaByExternId(nodeId)).thenReturn(None)
    when(imageApiService.importImage(nodeId)).thenReturn(None)

    val (result, requiredLibraries, errors) = ImageConverter.convert(content)

    result should equal (expectedResult)
    errors.length should be > 0
    requiredLibraries.length should equal(0)
  }
}
