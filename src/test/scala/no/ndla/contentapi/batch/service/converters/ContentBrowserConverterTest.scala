package no.ndla.contentapi.batch.service.converters

import no.ndla.learningpathapi.UnitSuite
import org.jsoup.Jsoup


class ContentBrowserConverterTest extends UnitSuite {

  test("That content-browser strings are replaced") {
    val initialContent = "<article><p>[contentbrowser ==nid=105448==imagecache=Fullbredde==width===alt=Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi.==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]&nbsp;</p></article>"
    val expectedResult = "<article> <p>{CONTENT-105448}</p></article>"
    val element = ContentBrowserConverter.convert(Jsoup.parseBodyFragment(initialContent).body().child(0))

    element.outerHtml().replace("\n", "") should equal (expectedResult)
  }
}
