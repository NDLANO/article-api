/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import no.ndla.articleapi.integration._
import no.ndla.articleapi.model._

import scala.util.{Failure, Success}

trait ExtractServiceComponent {
  this: MigrationApiClient with TagsService =>

  val extractService: ExtractService

  class ExtractService {
    def getNodeData(nodeId: String): NodeToConvert = {
      migrationApiClient.getContentNodeData(nodeId) match {
        case Success(data) => data.asNodeToConvert(nodeId, tagsService.forContent(nodeId))
        case Failure(ex) => throw ex
      }
    }

    def getNodeType(nodeId: String): Option[String] =
      migrationApiClient.getContentType(nodeId).map(x => x.nodeType).toOption

    def getNodeEmbedUrl(nodeId: String): Option[String] =
      migrationApiClient.getNodeEmbedData(nodeId).map(x => x.url).toOption

    def getNodeEmbedCode(nodeId: String): Option[String] =
      migrationApiClient.getNodeEmbedData(nodeId).map(x => x.embedCode).toOption

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

    def getBiblioMeta(nodeId: String): Option[BiblioMeta] =
      migrationApiClient.getBiblioMeta(nodeId).map(x => x.asBiblioMeta).toOption
  }
}
