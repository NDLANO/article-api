package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.{TestEnvironment, UnitSuite}

class H5PConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val content = ContentBrowser(contentString, Some("nb"), 1)

  test("That contentbrowser strings of type 'h5p_content' returns an iframe") {
    val expectedResult = """<figure data-resource="h5p" data-id="1" data-url="http://ndla.no/h5p/embed/1234"></figure>"""
    val (result, requiredLibraries, errors) = H5PConverter.convert(content)

    result should equal(expectedResult)
    errors.length should equal(0)
    requiredLibraries.length should be > 0
  }
}
