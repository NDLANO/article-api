package no.ndla.contentapi.service

import no.ndla.contentapi.integration.{CMDataComponent, ContentOppgave, ContentFagstoff}
import no.ndla.contentapi.model.ContentInformation

trait ExtractServiceComponent {
  this: CMDataComponent =>

  val extractService: ExtractService

  class ExtractService {
    def importNode(nodeId: String): ContentInformation = cmData.getNode(nodeId)
    def getNodeType(nodeId: String): Option[String] = cmData.getNodeType(nodeId)
    def getNodeEmbedData(nodeId: String): Option[(String, String)] = cmData.getNodeEmbedData(nodeId)
    def getNodeFagstoff(nodeId: String): Seq[ContentFagstoff] = {
      val fagstoffs = cmData.getNodeFagstoff(nodeId)

      // make sure to return the fagstoff along with all its translations
      fagstoffs.exists {x => x.isMainNode} match {
        case true => fagstoffs
        case false => if (fagstoffs.nonEmpty) cmData.getNodeFagstoff(fagstoffs(0).tnid) else fagstoffs
      }
    }

    def getNodeOppgave(nodeId: String): Seq[ContentOppgave] = {
      val oppgaves = cmData.getNodeOppgave(nodeId)

      // make sure to return the oppgave along with all its translations
      oppgaves.exists {x => x.isMainNode} match {
        case true => oppgaves
        case false => if (oppgaves.nonEmpty) cmData.getNodeOppgave(oppgaves(0).tnid) else oppgaves
      }
    }
  }
}
