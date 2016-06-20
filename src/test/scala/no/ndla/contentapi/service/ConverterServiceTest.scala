package no.ndla.contentapi.service

import no.ndla.contentapi.TestEnvironment
import no.ndla.contentapi.model._
import no.ndla.learningpathapi.UnitSuite
import org.mockito.Mockito._

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  val service = new ConverterService

  val contentTitle = ContentTitle("", Some(""))
  val license = License("licence", "description", Some("http://"))
  val author = Author("forfatter", "Henrik")
  val copyright = Copyright(license, "", List(author))
  val tag = ContentTag("asdf", Some("nb"))
  val requiredLibrary = RequiredLibrary("", "", "")
  val nodeId = "1234"
  val sampleAlt = "Fotografi"
  val sampleContentString = s"[contentbrowser ==nid=${nodeId}==imagecache=Fullbredde==width===alt=Foto==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"


  test("That the document is wrapped in an article tag") {
    val initialContent = "<h1>Heading</h1>"
    val node = ContentInformation("1", List(contentTitle), List(Content(initialContent, Some("nb"))), copyright, List(tag), List[RequiredLibrary]())
    val expedtedResult = "<article>" + initialContent + "</article>"

    service.convertNode(node)._1.content(0).content.replace("\n", "").replace(" ", "") should equal (expedtedResult)
  }

  test("That the updated ContentInformation contains requiredLibraries") {
    val initialContent = s"<p>$sampleContentString</p>"
    val node = ContentInformation(nodeId, List(contentTitle), List(Content(initialContent, Some("nb"))), copyright, List(tag), List[RequiredLibrary]())

    when(extractService.getNodeType(nodeId)).thenReturn((Some("video")))
    val (result, messages) = service.convertNode((node))
    result.requiredLibraries.length should equal(1)
  }
}
