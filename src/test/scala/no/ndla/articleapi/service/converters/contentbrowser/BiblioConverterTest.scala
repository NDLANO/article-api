package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class BiblioConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val (linkUrl, linkEmbedCode) = ("https://www.youtube.com/watch?v=1qN72LEQnaU", """<iframe src="https://www.youtube.com/embed/1qN72LEQnaU?feature=oembed"></iframe>""")

  test("That BiblioConverter replaces contentbrowser strings with an a tag containing the nodeId") {
    val insertion = "inline"
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=$insertion==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val content = ContentBrowser(contentString, Some("nb"))
    val expectedResult = s"""<a id="biblio-${content.get("nid")}"></a>"""
    val (result, requiredLibraries, importStatus) = BiblioConverter.convert(content, Seq())

    result should equal (expectedResult)
    requiredLibraries.isEmpty should be (true)
    importStatus.messages.isEmpty should be (true)
  }
}
