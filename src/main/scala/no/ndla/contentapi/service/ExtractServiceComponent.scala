package no.ndla.contentapi.service

import no.ndla.contentapi.integration._

trait ExtractServiceComponent {
  this: CMDataComponent =>

  val extractService: ExtractService

  class ExtractService {
    def getNodeData(nodeId: String): NodeToConvert = cmData.getNode(nodeId)
    def getNodeType(nodeId: String): Option[String] = cmData.getNodeType(nodeId)
    def getNodeEmbedData(nodeId: String): Option[String] = cmData.getNodeEmbedData(nodeId)

    def getAudioMeta(nodeId: String): Option[AudioMeta] = cmData.getAudioMeta(nodeId)

    def getNodeGeneralContent(nodeId: String): Seq[NodeGeneralContent] = {
      val content = cmData.getNodeGeneralContent(nodeId)

      // make sure to return the content along with all its translations
      content.exists {x => x.isMainNode} match {
        case true => content
        case false => if (content.nonEmpty) cmData.getNodeGeneralContent(content.head.tnid) else content
      }
    }

    def getNodeIngress(nodeId: String): Option[NodeIngress] = cmData.getNodeIngress(nodeId)
  }
}
