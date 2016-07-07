package no.ndla.contentapi.service

import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import no.ndla.contentapi.integration.NodeGeneralContent
import org.mockito.Mockito._

class ExtractServiceComponentTest extends UnitSuite with TestEnvironment {
  override val extractService = new ExtractService

  val (nodeId1, nodeId2) = ("111", "222")
  val oppgave1 = NodeGeneralContent(nodeId1, nodeId1, "tittel", "oppgave", "nb")
  val oppgave2 = NodeGeneralContent(nodeId2, nodeId1, "tittel", "oppgåve", "nn")

  test("That getNodeOppgave returns all translations of a node when requested node is main node") {
    when(cmData.getNodeGeneralContent(nodeId1)).thenReturn(List(oppgave1, oppgave2))
    when(cmData.getNodeGeneralContent(nodeId2)).thenReturn(List(oppgave2))

    extractService.getNodeGeneralContent(nodeId1) should equal (List(oppgave1, oppgave2))
  }

  test("That getNodeOppgave returns all translations of a node when requested node is a translation") {
    when(cmData.getNodeGeneralContent(nodeId1)).thenReturn(List(oppgave1, oppgave2))
    when(cmData.getNodeGeneralContent(nodeId2)).thenReturn(List(oppgave2))

    extractService.getNodeGeneralContent(nodeId2) should equal (List(oppgave1, oppgave2))
  }
}
