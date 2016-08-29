/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.{TestEnvironment, UnitSuite}


class NonExistentNodeConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=Melon==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"

  test("That NonExistentNodeConverter returns an empty string and a message") {
    val content = ContentBrowser(contentString, Some("nb"))
    val (result, requiredLibraries, status) = NonExistentNodeConverter.convert(content, Seq())

    result should equal ("")
    status.messages.nonEmpty should be (true)
    status.messages.head should equal (s"Found nonexistant node with id ${content.get("nid")}")
  }
}
