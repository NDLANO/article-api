package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.{TestEnvironment, UnitSuite}


class NonExistentNodeConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=Melon==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"

  test("That NonExistentNodeConverter returns an empty string and a message") {
    val content = ContentBrowser(contentString, Some("nb"))
    val (result, requiredLibraries, messages) = NonExistentNodeConverter.convert(content)

    result should equal ("")
    messages.nonEmpty should be (true)
  }
}
