/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.model.domain.ImportStatus
import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class NonExistentNodeConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=Melon==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"

  test("That NonExistentNodeConverter returns a Failure") {
    val content = ContentBrowser(contentString, "nb")
    NonExistentNodeConverter.convert(content, ImportStatus.empty).isFailure should be (true)
  }
}
