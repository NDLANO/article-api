/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.integration.MigrationApiClient
import no.ndla.articleapi.model.api.NotFoundException
import no.ndla.articleapi.model.domain.{Article, ImportStatus, NodeToConvert}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.search.IndexService

import scala.util.{Failure, Success, Try}

trait ExtractConvertStoreContent {
  this: ExtractService with MigrationApiClient with ConverterService with ArticleRepository with IndexService =>

  val extractConvertStoreContent: ExtractConvertStoreContent

  class ExtractConvertStoreContent extends LazyLogging {
    def processNode(externalId: String, importStatus: ImportStatus = ImportStatus(Seq(), Seq())): Try[(Long, ImportStatus)] = {
      if (importStatus.visitedNodes.contains(externalId))
        return articleRepository.getIdFromExternalId(externalId) match {
          case Some(id) => Success(id, importStatus)
          case None => Failure(NotFoundException(s"Content with external id $externalId was not found"))
        }

      extract(externalId) map { case (node, mainNodeId) =>
        val (convertedNode, updatedImportStatus) = convert(node, importStatus)
        val newId = store(convertedNode, mainNodeId)
        val indexErrors = indexArticle(convertedNode.copy(id=Some(newId)))
        (newId, updatedImportStatus ++ ImportStatus(Seq(s"Successfully imported node $externalId: $newId") ++ indexErrors))
      }
    }

    private def extract(externalId: String): Try[(NodeToConvert, String)] = {
      val node = extractService.getNodeData(externalId)
      node.contents.find(_.isMainNode) match {
        case None => Failure(NotFoundException(s"$externalId is a translation; Could not find main node"))
        case Some(mainNode) => Success(node, mainNode.nid)
      }
    }

    private def convert(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (Article, ImportStatus) =
      converterService.toDomainArticle(nodeToConvert, importStatus)

    private def store(article: Article, mainNodeNid: String): Long = {
      val subjectIds = getSubjectIds(mainNodeNid)
      articleRepository.exists(mainNodeNid) match {
        case true => articleRepository.updateWithExternalId(article, mainNodeNid)
        case false => articleRepository.insertWithExternalIds(article, mainNodeNid, subjectIds)
      }
    }

    private def indexArticle(article: Article): Seq[String] = {
      Try(indexService.indexDocument(article)) match {
        case Failure(f) => Seq(s"Failed to index article with id ${article.id}: ${f.getMessage}")
        case Success(_) => Seq()
      }
    }

    private def getSubjectIds(nodeId: String): Seq[String] =
      migrationApiClient.getSubjectForNode(nodeId) match {
        case Failure(ex) => Seq()
        case Success(subjectMetas) => subjectMetas.map(_.nid)
      }
  }
}
