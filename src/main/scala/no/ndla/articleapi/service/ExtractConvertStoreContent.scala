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
import no.ndla.articleapi.model.api.{ImportException, NotFoundException}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.repository.{ArticleRepository, ConceptRepository}
import no.ndla.articleapi.service.search.{ArticleIndexService, ConceptIndexService}
import no.ndla.articleapi.ArticleApiProperties.supportedContentTypes
import no.ndla.articleapi.validation.ContentValidator
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

trait ExtractConvertStoreContent {
  this: ExtractService
    with MigrationApiClient
    with ConverterService
    with ArticleRepository
    with ConceptRepository
    with ArticleIndexService
    with ConceptIndexService
    with ReadService
    with ContentValidator =>

  val extractConvertStoreContent: ExtractConvertStoreContent

  class ExtractConvertStoreContent extends LazyLogging {
    def processNode(externalId: String, forceUpdateArticles: Boolean): Try[(Content, ImportStatus)] =
      processNode(externalId, ImportStatus.empty, forceUpdateArticles)

    def processNode(externalId: String, importStatus: ImportStatus = ImportStatus.empty, forceUpdateArticles: Boolean = false): Try[(Content, ImportStatus)] = {
      if (importStatus.visitedNodes.contains(externalId)) {
        return getMainNodeId(externalId).flatMap(readService.getContentByExternalId) match {
          case Some(content) => Success(content, importStatus)
          case None => Failure(NotFoundException(s"Content with external id $externalId was not found"))
        }
      }

      val importedArticle = for {
        (node, mainNodeId) <- extract(externalId)
        (convertedContent, updatedImportStatus) <- converterService.toDomainArticle(node, importStatus)
        _ <- importValidator.validate(convertedContent, allowUnknownLanguage=true)
        content <- store(convertedContent, mainNodeId, forceUpdateArticles)
        _ <- indexContent(content)
      } yield (content, updatedImportStatus.addMessage(s"Successfully imported node $externalId: ${content.id.get}").setArticleId(content.id.get))

      if (importedArticle.isFailure) {
        deleteArticleByExternalId(externalId)
      }

      importedArticle
    }

    private def deleteArticleByExternalId(externalId: String) = {
      articleRepository.getIdFromExternalId(externalId).map(articleId => {
        logger.info(s"Deleting previously imported article (id=$articleId, external id=$externalId) from database because the article could not be re-imported")
        articleRepository.delete(articleId)
        articleIndexService.deleteDocument(articleId)
      })
    }

    private def getMainNodeId(externalId: String): Option[String] = {
      extract(externalId) map { case (_, mainNodeId) => mainNodeId } toOption
    }

    private def extract(externalId: String): Try[(NodeToConvert, String)] = {
      val node = extractService.getNodeData(externalId)
      node.contents.find(_.isMainNode) match {
        case None => Failure(NotFoundException(s"$externalId is a translation; Could not find main node"))
        case Some(mainNode) =>
          if (supportedContentTypes.contains(node.nodeType.toLowerCase) || supportedContentTypes.contains(node.contentType.toLowerCase))
            Success(node, mainNode.nid)
          else
            Failure(ImportException(s"Tried to import node of unsupported type '${node.nodeType.toLowerCase}/${node.contentType.toLowerCase()}'"))
      }
    }

    private def store(content: Content, mainNodeId: String, forceUpdateArticle: Boolean): Try[Content] = {
      content match {
        case article: Article => storeArticle(article, mainNodeId, forceUpdateArticle)
        case concept: Concept => storeConcept(concept, mainNodeId)
      }
    }

    private def storeArticle(article: Article, mainNodeNid: String, forceUpdateArticle: Boolean): Try[Content] = {
      val subjectIds = getSubjectIds(mainNodeNid)
      articleRepository.exists(mainNodeNid) match {
        case true if !forceUpdateArticle => articleRepository.updateWithExternalId(article, mainNodeNid)
        case true if forceUpdateArticle => articleRepository.updateWithExternalIdOverrideManualChanges(article, mainNodeNid)
        case false => Try(articleRepository.insertWithExternalIds(article, mainNodeNid, subjectIds))
      }
    }

    private def storeConcept(concept: Concept, mainNodeNid: String): Try[Content] = {
      conceptRepository.exists(mainNodeNid) match {
        case true => conceptRepository.updateWithExternalId(concept, mainNodeNid)
        case false => Try(conceptRepository.insertWithExternalId(concept, mainNodeNid))
      }
    }

    private def indexContent(content: Content): Try[Content] = {
      content match {
        case a: Article => articleIndexService.indexDocument(a)
        case c: Concept => conceptIndexService.indexDocument(c)
      }
    }

    private def getSubjectIds(nodeId: String): Seq[String] =
      migrationApiClient.getSubjectForNode(nodeId) match {
        case Failure(ex) => Seq()
        case Success(subjectMetas) => subjectMetas.map(_.nid)
      }

  }
}
