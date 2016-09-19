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
import no.ndla.articleapi.model._
import scala.annotation.tailrec

trait ConverterServiceComponent {
  this: ConverterModules with ExtractConvertStoreContent with ImageApiServiceComponent =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    def toArticleInformation(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (ArticleInformation, ImportStatus) = {
      val updatedVisitedNodes = importStatus.visitedNodes ++ nodeToConvert.contents.map(_.nid)
      val (convertedContent, converterStatus) = convert(nodeToConvert, maxConvertionRounds, importStatus.copy(visitedNodes = updatedVisitedNodes.distinct))
      val (postProcessed, postProcessStatus) = postProcess(convertedContent, converterStatus)

      val (articleInformation, toArticleStatus) = toArticleInformation(postProcessed)
      (articleInformation, postProcessStatus ++ toArticleStatus)
    }

    @tailrec private def convert(nodeToConvert: NodeToConvert, maxRoundsLeft: Int, importStatus: ImportStatus): (NodeToConvert, ImportStatus) = {
      if (maxRoundsLeft == 0) {
        val message = "Maximum number of converter rounds reached; Some content might not be converted"
        logger.warn(message)
        return (nodeToConvert, importStatus.copy(messages=importStatus.messages :+ message))
      }

      val (updatedContent, updatedStatus) = executeConverterModules(nodeToConvert, importStatus)

      // If this converting round did not yield any changes to the content, this node is finished (case true)
      // If changes were made during this convertion, we run the converters again (case false)
      updatedContent == nodeToConvert match {
        case true => (updatedContent, updatedStatus)
        case false => convert(updatedContent, maxRoundsLeft - 1, updatedStatus)
      }
    }

    private def postProcess(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (NodeToConvert, ImportStatus) =
      executePostprocessorModules(nodeToConvert, importStatus)

    private def toArticleIngress(nodeIngress: NodeIngress): (ArticleIntroduction, ImportStatus) = {
      val newImageId = nodeIngress.imageNid.flatMap(imageApiService.importOrGetMetaByExternId).map(_.id)

      val importStatus = (nodeIngress.imageNid, newImageId) match {
        case (Some(imageNid), None) => ImportStatus(s"Failed to import ingress image with external id $imageNid", Seq())
        case _ => ImportStatus(Seq(), Seq())
      }

      (ArticleIntroduction(nodeIngress.content, newImageId, nodeIngress.ingressVisPaaSiden == 1, nodeIngress.language), importStatus)
    }

    private def toArticleInformation(nodeToConvert: NodeToConvert): (ArticleInformation, ImportStatus) = {
      val requiredLibraries = nodeToConvert.contents.flatMap(_.requiredLibraries).distinct
      val (ingresses, importStatuses) = nodeToConvert.ingress.map(toArticleIngress).unzip

      (ArticleInformation("0",
        nodeToConvert.titles,
        nodeToConvert.contents.map(_.asContent),
        nodeToConvert.copyright,
        nodeToConvert.tags,
        requiredLibraries,
        nodeToConvert.visualElements,
        ingresses,
        nodeToConvert.created,
        nodeToConvert.updated,
        nodeToConvert.contentType), ImportStatus(importStatuses))
    }

  }
}
