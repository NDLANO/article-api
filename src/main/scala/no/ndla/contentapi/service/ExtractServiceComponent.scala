package no.ndla.contentapi.service

import no.ndla.contentapi.integration._
import no.ndla.contentapi.model._

import scala.util.{Failure, Success}

trait ExtractServiceComponent {
  this: MigrationApiClient =>

  val extractService: ExtractService

  class ExtractService {
    def getNodeData(nodeId: String): NodeToConvert = {
      migrationApiClient.getContentNodeData(nodeId) match {
        case Success(data) => data.asNodeToConvert(nodeId)
        case Failure(ex) => throw ex
      }
    }

    def getNodeType(nodeId: String): Option[String] =
      migrationApiClient.getContentType(nodeId).map(x => x.nodeType).toOption

    def getNodeEmbedData(nodeId: String): Option[String] =
      migrationApiClient.getNodeEmbedData(nodeId).map(x => x.embed).toOption

    def getAudioMeta(nodeId: String): Option[ContentFilMeta] =
      migrationApiClient.getAudioMeta(nodeId).map(x => x.asContentFilMeta).toOption

    def getNodeFilMeta(nodeId: String): Option[ContentFilMeta] =
      migrationApiClient.getFilMeta(nodeId).map(x => x.asContentFilMeta).toOption

    def getNodeGeneralContent(nodeId: String): Seq[NodeGeneralContent] = {
      val content = migrationApiClient.getNodeGeneralContent(nodeId).getOrElse(Seq()).map(x => x.asNodeGeneralContent)

      // make sure to return the content along with all its translations
      content.exists {x => x.isMainNode} match {
        case true => content
        case false => if (content.nonEmpty)
          migrationApiClient.getNodeGeneralContent(content.head.tnid).getOrElse(Seq()).map(x => x.asNodeGeneralContent)
        else
          content
      }
    }

    def getNodeIngress(nodeId: String): Option[NodeIngress] =
      migrationApiClient.getContentNodeData(nodeId) match {
        case Success(data) => data.ingresses.find(x => x.nid == nodeId).map(x => x.asNodeIngress)
        case Failure(ex) => None
      }

    def getBiblioMeta(nodeId: String): Option[BiblioMeta] =
      migrationApiClient.getBiblioMeta(nodeId).map(x => x.asBiblioMeta).toOption
  }
}
