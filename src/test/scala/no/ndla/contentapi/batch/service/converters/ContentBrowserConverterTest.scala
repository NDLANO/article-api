package no.ndla.contentapi.batch.service.converters

import no.ndla.contentapi.batch.BatchTestEnvironment
import no.ndla.learningpathapi.UnitSuite
import org.jsoup.Jsoup
import org.mockito.Mockito._

class ContentBrowserConverterTest extends UnitSuite with BatchTestEnvironment {
  override def beforeEach() {

  }

  test("That content-browser strings are replaced") {
    val nodeId = "1234"
    val initialContent = s"<article><p>[contentbrowser ==nid=${nodeId}==imagecache=Fullbredde==width===alt=Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi.==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]</p></article>"
    val expectedResult = s"<article> <p>{CONTENT-${nodeId}}</p></article>"

    when(extractService.getNodeType(nodeId)).thenReturn(Some(""))
    val element = contentBrowserConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    element.outerHtml().replace("\n", "") should equal (expectedResult)
  }

  test("That content-browser strings of type h5p_content are converted correctly") {
    val nodeId = "1234"
    val initialContent = s"<article>[contentbrowser ==nid=${nodeId}==imagecache=Fullbredde==width===insertion=inline==link_title_text=Struktur og organisering av innhold==lightbox_size===link_text=Struktur og organisering av innhold==text_align===alt===css_class=contentbrowser contentbrowser]</article>"
    val expectedResult = s"""<article> <embed data-oembed="http://ndla.no/node/${nodeId}" /></article>"""

    when(extractService.getNodeType(nodeId)).thenReturn(Some("h5p_content"))
    val element = contentBrowserConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    element.outerHtml().replace("\n", "") should equal (expectedResult)
  }
}
