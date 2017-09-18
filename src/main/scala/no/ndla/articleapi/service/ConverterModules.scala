/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import no.ndla.articleapi.integration.ConverterModule
import no.ndla.articleapi.model.api.ImportExceptions
import no.ndla.articleapi.model.domain.{ImportStatus, NodeToConvert}
import no.ndla.articleapi.ArticleApiProperties.{nodeTypeBegrep, nodeTypeVideo}

import scala.util.{Failure, Success, Try}

case class ConverterPipeLine(mainConverters: Seq[ConverterModule], postProcessorConverters: Seq[ConverterModule])

trait ConverterModules {
  val articleConverter: ConverterPipeLine
  val conceptConverter: ConverterPipeLine
  val leafNodeConverter: ConverterPipeLine

  def executeConverterModules(nodeToConvert: NodeToConvert, importStatus: ImportStatus): Try[(NodeToConvert, ImportStatus)] = {
    val modulesToRun = nodeToConvert.nodeType.toLowerCase match {
      case `nodeTypeBegrep` => conceptConverter.mainConverters
      case `nodeTypeVideo` => leafNodeConverter.mainConverters
      case _ => articleConverter.mainConverters
    }
    runConverters(modulesToRun, nodeToConvert, importStatus)
  }

  def executePostprocessorModules(nodeToConvert: NodeToConvert, importStatus: ImportStatus): Try[(NodeToConvert, ImportStatus)] = {
    val modulesToRun = nodeToConvert.nodeType.toLowerCase match {
      case `nodeTypeBegrep` => conceptConverter.postProcessorConverters
      case `nodeTypeVideo` => leafNodeConverter.postProcessorConverters
      case _ => articleConverter.postProcessorConverters
    }
    runConverters(modulesToRun, nodeToConvert, importStatus)
  }

  private def runConverters(converters: Seq[ConverterModule], nodeToConvert: NodeToConvert, importStatus: ImportStatus): Try[(NodeToConvert, ImportStatus)] = {
     val (convertedNode, finalImportStatus, exceptions) = converters.foldLeft((nodeToConvert, importStatus, Seq[Throwable]()))((element, converter) => {

      val (partiallyConvertedNode, importStatus, exceptions) = element
      converter.convert(partiallyConvertedNode, importStatus) match {
        case Success((updatedNode, updatedImportStatus)) => (updatedNode, updatedImportStatus, exceptions)
        case Failure(x) => (partiallyConvertedNode, importStatus, exceptions :+ x)
      }
    })

    if (exceptions.nonEmpty) {
      val failedNodeIds = nodeToConvert.contents.map(_.nid).mkString(",")
      return Failure(new ImportExceptions(s"Error importing node(s) with id(s) $failedNodeIds", errors=exceptions))
    }

    Success((convertedNode, finalImportStatus))
  }


}
