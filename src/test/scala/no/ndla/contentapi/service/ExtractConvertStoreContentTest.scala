package no.ndla.contentapi.service

import no.ndla.contentapi.integration.LanguageContent
import no.ndla.contentapi.model._
import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.Matchers._
import scalikejdbc.DBSession

import scala.util.{Success, Try}

class ExtractConvertStoreContentTest extends UnitSuite with TestEnvironment {
  val (nodeId, nodeId2) = ("1234", "4321")
  val sampleTitle = ContentTitle("title", Some("en"))
  val contentString = s"[contentbrowser ==nid=$nodeId2==imagecache=Fullbredde==width===alt=alttext==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=link==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
  val contentString2 = s"[contentbrowser ==nid=$nodeId2==imagecache=Fullbredde==width===alt=alttext==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=link==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
  val sampleContent = LanguageContent(nodeId, nodeId, contentString, Some("en"))
  val sampleContent2 = LanguageContent(nodeId, nodeId2, "content", Some("en"))
  val license = License("licence", "description", Some("http://"))
  val author = Author("forfatter", "Henrik")
  val copyright = Copyright(license, "", List(author))

  val sampleNode = NodeToConvert(List(sampleTitle), List(sampleContent), copyright, List(ContentTag(List("tag"), Some("en"))))
  val sampleNode2 = sampleNode.copy(contents=List(sampleContent2))

  val eCSService = new ExtractConvertStoreContent

  test("That ETL extracts, translates and loads a node correctly") {
    val newNodeid: Long = 4444
    val ingress =  NodeIngress(nodeId, "ingress here", None, 0)

    when(extractService.getNodeData(nodeId)).thenReturn(sampleNode)
    when(extractService.getNodeIngress(nodeId)).thenReturn(Some(ingress))
    when(extractService.getNodeType(nodeId2)).thenReturn(Some("fagstoff"))
    when(extractService.getNodeGeneralContent(nodeId2)).thenReturn(Seq(NodeGeneralContent(nodeId2, nodeId2, "title", "content", "en")))
    when(contentRepository.withExternalId(nodeId2)).thenReturn(None)
    when(extractConvertStoreContent.processNode(nodeId2, ImportStatus(Seq(), Seq(nodeId)))).thenReturn(Try((newNodeid, ImportStatus(Seq(), Seq(nodeId, nodeId2)))))

    when(contentRepository.exists(sampleNode.contents.head.nid)).thenReturn(false)
    when(contentRepository.insert(any[ContentInformation], any[String])(any[DBSession])).thenReturn(newNodeid)

    eCSService.processNode(nodeId) should equal(Success(newNodeid, ImportStatus(List(), List(nodeId, nodeId2))))
  }

  test("That ETL returns a list of visited nodes") {
    val newNodeid: Long = 4444

    when(extractService.getNodeData(nodeId)).thenReturn(sampleNode)
    when(extractService.getNodeIngress(nodeId)).thenReturn(None)
    when(extractService.getNodeType(nodeId2)).thenReturn(Some("fagstoff"))
    when(extractService.getNodeGeneralContent(nodeId2)).thenReturn(Seq(NodeGeneralContent(nodeId2, nodeId2, "title", "content", "en")))
    when(contentRepository.withExternalId(nodeId2)).thenReturn(None)
    when(extractConvertStoreContent.processNode(nodeId2, ImportStatus(Seq(), Seq("9876", nodeId)))).thenReturn(Try((newNodeid, ImportStatus(Seq(), Seq("9876", nodeId, nodeId2)))))

    when(contentRepository.exists(sampleNode.contents.head.nid)).thenReturn(false)
    when(contentRepository.insert(any[ContentInformation], any[String])(any[DBSession])).thenReturn(newNodeid)

    val result = eCSService.processNode(nodeId, ImportStatus(Seq(), Seq("9876")))
    result should equal(Success(newNodeid, ImportStatus(List(), List("9876", nodeId, nodeId2))))
  }

}
