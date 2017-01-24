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
import no.ndla.articleapi.service.search.SearchIndexService

import scala.util.{Failure, Success, Try}

trait ExtractConvertStoreContent {
  this: ExtractService with MigrationApiClient with ConverterService with ArticleRepository with SearchIndexService =>

  val extractConvertStoreContent: ExtractConvertStoreContent

  class ExtractConvertStoreContent extends LazyLogging {
    def processNode(externalId: String, importStatus: ImportStatus = ImportStatus(Seq(), Seq())): Try[(Long, ImportStatus)] = {
      if (importStatus.visitedNodes.contains(externalId)) {
        return articleRepository.getIdFromExternalId(externalId) match {
          case Some(id) => Success(id, importStatus)
          case None => Failure(NotFoundException(s"Content with external id $externalId was not found"))
        }
      }

      for {
        (node, mainNodeId) <- extract(externalId)
        (convertedArticle, updatedImportStatus) <- convert(node, importStatus)
        newId <- store(convertedArticle, mainNodeId)
        _ <- searchIndexService.indexDocument(convertedArticle.copy(id = Some(newId)))
      } yield (newId, updatedImportStatus ++ ImportStatus(Seq(s"Successfully imported node $externalId: $newId")))
    }

    private def extract(externalId: String): Try[(NodeToConvert, String)] = {
      val node = extractService.getNodeData(externalId)
      node.contents.find(_.isMainNode) match {
        case None => Failure(NotFoundException(s"$externalId is a translation; Could not find main node"))
        case Some(mainNode) => Success(node, mainNode.nid)
      }
    }

    private def convert(nodeToConvert: NodeToConvert, importStatus: ImportStatus): Try[(Article, ImportStatus)] =
      Success(converterService.toDomainArticle(nodeToConvert, importStatus))

    private def store(article: Article, mainNodeNid: String): Try[Long] = {
      val subjectIds = getSubjectIds(mainNodeNid)
      articleRepository.exists(mainNodeNid) match {
        case true => articleRepository.updateWithExternalId(article, mainNodeNid)
        case false => Success(articleRepository.insertWithExternalIds(article, mainNodeNid, subjectIds))
      }
    }

    private def getSubjectIds(nodeId: String): Seq[String] =
      migrationApiClient.getSubjectForNode(nodeId) match {
        case Failure(ex) => Seq()
        case Success(subjectMetas) => subjectMetas.map(_.nid)
      }
  }

}
