package no.ndla.contentapi.service

import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import no.ndla.contentapi.integration.ContentOppgave
import org.mockito.Mockito._

class ExtractServiceComponentTest extends UnitSuite with TestEnvironment {
  override val extractService = new ExtractService

  val (nodeId1, nodeId2) = ("111", "222")
  val oppgave1 = ContentOppgave(nodeId1, nodeId1, "tittel", "oppgave", "nb")
  val oppgave2 = ContentOppgave(nodeId2, nodeId1, "tittel", "oppgåve", "nn")

  test("That getNodeOppgave returns all translations of a node when requested node is main node") {
    when(cmData.getNodeOppgave(nodeId1)).thenReturn(List(oppgave1, oppgave2))
    when(cmData.getNodeOppgave(nodeId2)).thenReturn(List(oppgave2))

    extractService.getNodeOppgave(nodeId1) should equal (List(oppgave1, oppgave2))
  }

  test("That getNodeOppgave returns all translations of a node when requested node is a translation") {
    when(cmData.getNodeOppgave(nodeId1)).thenReturn(List(oppgave1, oppgave2))
    when(cmData.getNodeOppgave(nodeId2)).thenReturn(List(oppgave2))

    extractService.getNodeOppgave(nodeId2) should equal (List(oppgave1, oppgave2))
  }
}
