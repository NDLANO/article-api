package no.ndla.contentapi.service

import no.ndla.contentapi.integration.CMDataComponent
import no.ndla.contentapi.model.ContentInformation

trait ExtractServiceComponent {
  this: CMDataComponent =>

  val extractService: ExtractService

  class ExtractService {
    def importNode(nodeId: String): ContentInformation = cmData.getNode(nodeId)
    def getNodeType(nodeId: String): Option[String] = cmData.getNodeType(nodeId)
    def getNodeEmbedData(nodeId: String): Option[(String, String)] = cmData.getNodeEmbedData(nodeId)
    def getNodeAktualitet(nodeId: String): Option[NodeAktualitet] = None // TODO
  }
}

case class NodeAktualitet(nid: String, tnid: String, language: String, title: String, aktualitet: String)
