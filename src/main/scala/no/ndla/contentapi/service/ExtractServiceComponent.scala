package no.ndla.contentapi.service

import no.ndla.contentapi.integration._

trait ExtractServiceComponent {
  this: CMDataComponent =>

  val extractService: ExtractService

  class ExtractService {
    def importNode(nodeId: String): NodeToConvert = cmData.getNode(nodeId)
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
    def getAudioMeta(nodeId: String): Option[ContentFilMeta] = cmData.getAudioMeta(nodeId)

    def getNodeAktualitet(nodeId: String): Seq[ContentAktualitet] = {
      val aktualitets = cmData.getNodeAktualitet(nodeId)

      // make sure to return the aktualitet along with all its translations
      aktualitets.exists {x => x.isMainNode} match {
        case true => aktualitets
        case false => if (aktualitets.nonEmpty) cmData.getNodeAktualitet(aktualitets(0).tnid) else aktualitets
      }
    }

    def getNodeIngress(nodeId: String): Option[NodeIngress] = cmData.getNodeIngress(nodeId)

    def getNodeFilMeta(nodeId: String): Option[ContentFilMeta] = cmData.getNodeFilMeta(nodeId)
  }
}
