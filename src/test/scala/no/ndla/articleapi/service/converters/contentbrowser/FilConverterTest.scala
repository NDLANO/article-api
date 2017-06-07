/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.model.domain.ContentFilMeta
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import no.ndla.articleapi.model.domain.ContentFilMeta._
import no.ndla.articleapi.ArticleApiProperties.Domain

import scala.util.Success

class FilConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val title = "melon"
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=totoggram==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text=$title==text_align===css_class=contentbrowser contentbrowser]"

  test("That FilConverter returns a link to the file") {
    val content = ContentBrowser(contentString, Some("nb"))
    val fileMeta = ContentFilMeta(nodeId, "0", "title", "title.pdf", s"$Domain/files/title.pdf", "application/pdf", "1024")
    val filePath = fileMeta.fileName
    val expectedResult = s"""<a href="$Domain/files/$filePath" title="${fileMeta.fileName}">${fileMeta.fileName}</a>"""

    when(extractService.getNodeFilMeta(nodeId)).thenReturn(Success(fileMeta))
    when(attachmentStorageService.uploadFileFromUrl(fileMeta)).thenReturn(Success(filePath))
    val Success((result, _, _)) = FilConverter.convert(content, Seq())

    result should equal(expectedResult)
    verify(extractService, times(1)).getNodeFilMeta(nodeId)
    verify(attachmentStorageService, times(1)).uploadFileFromUrl(fileMeta)
  }
}
