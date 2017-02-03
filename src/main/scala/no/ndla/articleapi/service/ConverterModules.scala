/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import no.ndla.articleapi.integration.ConverterModule
import no.ndla.articleapi.model.api.{ImportException, ImportExceptions}
import no.ndla.articleapi.model.domain.{ImportStatus, NodeToConvert}

import scala.util.{Failure, Success, Try}

trait ConverterModules {
  val converterModules: Seq[ConverterModule]
  val postProcessorModules: Seq[ConverterModule]

  def executeConverterModules(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (NodeToConvert, ImportStatus) =
    runConverters(converterModules, nodeToConvert, importStatus)

  def executePostprocessorModules(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (NodeToConvert, ImportStatus) =
    runConverters(postProcessorModules, nodeToConvert, importStatus)

  private def runConverters(converters: Seq[ConverterModule], nodeToConvert: NodeToConvert, importStatus: ImportStatus): (NodeToConvert, ImportStatus) = {
     val (convertedNode, finalImportStatus, exceptions) = converters.foldLeft((nodeToConvert, importStatus, Seq[Throwable]()))((element, converter) => {

      val (partiallyConvertedNode, importStatus, exceptions) = element
      converter.convert(partiallyConvertedNode, importStatus) match {
        case Success((updatedNode, updatedImportStatus)) => (updatedNode, updatedImportStatus, exceptions)
        case Failure(x) => (partiallyConvertedNode, importStatus, exceptions :+ x)
      }
    })

    if (exceptions.nonEmpty) {
      val failedNodeIds = nodeToConvert.contents.map(_.nid).mkString(",")
      throw new ImportExceptions(s"Error importing node(s) with id(s) $failedNodeIds", errors=exceptions)
    }

    (convertedNode, finalImportStatus)
  }


}
