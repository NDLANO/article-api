package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.integration.ContentFilMeta
import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import no.ndla.contentapi.integration.ContentFilMeta._

class FilConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val title = "melon"
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=totoggram==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text=$title==text_align===css_class=contentbrowser contentbrowser]"

  test("That FilConverter returns a link to the file") {
    val content = ContentBrowser(contentString, Some("nb"))
    val fileMeta = ContentFilMeta(nodeId, "0", "title", "title.pdf", "http://path/to/title.pdf", "application/pdf", "1024")
    val filePath = s"/some/file/path/to/${fileMeta.fileName}"
    val expectedResult = s"""<a href="$filePath">${fileMeta.fileName}</a>"""

    when(extractService.getNodeFilMeta(nodeId)).thenReturn(Some(fileMeta))
    when(storageService.uploadFileFromUrl(nodeId, fileMeta)).thenReturn(Some(filePath))
    val (result, requiredLibraries, messages) = FilConverter.convert(content)

    result should equal(expectedResult)
    verify(extractService, times(1)).getNodeFilMeta(nodeId)
    verify(storageService, times(1)).uploadFileFromUrl(nodeId, fileMeta)
  }
}
