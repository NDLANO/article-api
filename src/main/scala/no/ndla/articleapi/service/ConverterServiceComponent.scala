/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties.maxConvertionRounds
import no.ndla.articleapi.integration.{MigrationRelatedContent, MigrationRelatedContents}
import no.ndla.articleapi.model._

import scala.annotation.tailrec
import scala.util.{Failure, Success}

trait ConverterServiceComponent {
  this: ConverterModules with ExtractConvertStoreContent with ImageApiServiceComponent =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    def convertNode(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (ArticleInformation, ImportStatus) = {
      @tailrec def convertNode(nodeToConvert: NodeToConvert, maxRoundsLeft: Int, importStatus: ImportStatus): (NodeToConvert, ImportStatus) = {
        if (maxRoundsLeft == 0) {
          val message = "Maximum number of converter rounds reached; Some content might not be converted"
          logger.warn(message)
          return (nodeToConvert, importStatus.copy(messages=importStatus.messages :+ message))
        }

        val (updatedContent, updatedStatus) = convert(nodeToConvert, importStatus)

        // If this converting round did not yield any changes to the content, this node is finished (case true)
        // If changes were made during this convertion, we run the converters again (case false)
        updatedContent == nodeToConvert match {
          case true => (updatedContent, updatedStatus)
          case false => convertNode(updatedContent, maxRoundsLeft - 1, updatedStatus)
        }
      }

      val updatedVisitedNodes = importStatus.visitedNodes ++ nodeToConvert.contents.map(_.nid)
      val (convertedContent, converterStatus) = convertNode(nodeToConvert, maxConvertionRounds, importStatus.copy(visitedNodes = updatedVisitedNodes.distinct))

      val (articleInformation, toArticleStatus) = toArticleInformation(convertedContent)
      (articleInformation, converterStatus ++ toArticleStatus)
    }

    private def convert(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (NodeToConvert, ImportStatus) =
      converterModules.foldLeft((nodeToConvert, importStatus))((element, converter) => {
        val (updatedNodeToConvert, importStatus) = element
        converter.convert(updatedNodeToConvert, importStatus)
      })

    def toRelatedContents(migrationRelatedContents: MigrationRelatedContents): (Option[RelatedContents], ImportStatus) = {
      def toRelatedContent(related: MigrationRelatedContent): (Option[RelatedContent], ImportStatus) = {
        extractConvertStoreContent.processNode(related.nid) match {
          case Success((id, importStatus)) => (Some(RelatedContent(id, related.title, related.uri, related.fagligRelation)), importStatus)
          case Failure(ex) => {
            val message = s"Failed to import related content with id ${related.nid}"
            logger.warn(message)
            (None, ImportStatus(message, Seq()))
          }
        }
      }

      val (relatedContent, importStatuses) = migrationRelatedContents.related.map(toRelatedContent(_)).unzip
      relatedContent.flatten.nonEmpty match {
        case true => (Some(RelatedContents(relatedContent.flatten, migrationRelatedContents.language)), ImportStatus(importStatuses))
        case false => (None, ImportStatus(importStatuses))
      }
    }

    def toArticleIngress(nodeIngress: NodeIngress): ArticleIngress = {
      val id = nodeIngress.imageNid.flatMap(imageApiService.importOrGetMetaByExternId).map(_.id)
      ArticleIngress(nodeIngress.content, id, nodeIngress.ingressVisPaaSiden == 1, nodeIngress.language)
    }

    def toArticleInformation(nodeToConvert: NodeToConvert): (ArticleInformation, ImportStatus) = {
      val requiredLibraries = nodeToConvert.contents.flatMap(_.requiredLibraries).distinct
      val ingresses = nodeToConvert.contents.flatMap(_.ingress).map(toArticleIngress)
      val (relatedContents, importStatuses) = nodeToConvert.relatedContents.map(toRelatedContents).unzip

      (ArticleInformation("0",
        nodeToConvert.titles,
        nodeToConvert.contents.map(_.asContent),
        nodeToConvert.copyright,
        nodeToConvert.tags,
        requiredLibraries,
        nodeToConvert.pageTitles,
        nodeToConvert.visualElements,
        ingresses.map(x => MetaImage(x.image, x.language)).filter(_.image.isDefined),
        ingresses,
        relatedContents.flatten), ImportStatus(importStatuses))
    }

  }
}
