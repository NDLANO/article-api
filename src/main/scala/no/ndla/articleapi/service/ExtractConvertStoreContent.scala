/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.{Article, ImportStatus, NodeNotFoundException, NodeToConvert}
import no.ndla.articleapi.repository.ArticleRepositoryComponent

import scala.util.{Failure, Success, Try}

trait ExtractConvertStoreContent {
  this: ExtractServiceComponent with ConverterServiceComponent with ArticleRepositoryComponent =>

  val extractConvertStoreContent: ExtractConvertStoreContent

  class ExtractConvertStoreContent extends LazyLogging {
    def processNode(externalId: String, importStatus: ImportStatus = ImportStatus(Seq(), Seq())): Try[(Long, ImportStatus)] = {
      if (importStatus.visitedNodes.contains(externalId))
        return articleRepository.getIdFromExternalId(externalId) match {
          case Some(id) => Success(id, importStatus)
          case None => Failure(NodeNotFoundException(s"Content with external id $externalId was not found"))
        }

      extract(externalId) map { case (node, mainNodeId) =>
        val (convertedNode, updatedImportStatus) = convert(node, importStatus)
        val newId = store(convertedNode, mainNodeId)
        (newId, updatedImportStatus ++ ImportStatus(Seq(s"Successfully imported node $externalId: $newId")))
      }
    }

    private def extract(externalId: String): Try[(NodeToConvert, String)] = {
      val node = extractService.getNodeData(externalId)
      node.contents.find(_.isMainNode) match {
        case None => Failure(NodeNotFoundException(s"$externalId is a translation; Could not find main node"))
        case Some(mainNode) => Success(node, mainNode.nid)
      }
    }

    private def convert(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (Article, ImportStatus) =
      converterService.toArticle(nodeToConvert, importStatus)

    private def store(article: Article, mainNodeNid: String): Long =
      articleRepository.exists(mainNodeNid) match {
        case true => articleRepository.update(article, mainNodeNid)
        case false => articleRepository.insert(article, mainNodeNid)
      }

  }
}
