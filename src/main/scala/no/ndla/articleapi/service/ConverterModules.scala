/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import no.ndla.articleapi.integration.ConverterModule
import no.ndla.articleapi.model.domain.{ImportStatus, NodeToConvert}

trait ConverterModules {
  val converterModules: Seq[ConverterModule]
  val postProcessorModules: Seq[ConverterModule]

  def executeConverterModules(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (NodeToConvert, ImportStatus) =
    runConverters(converterModules, nodeToConvert, importStatus)

  def executePostprocessorModules(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (NodeToConvert, ImportStatus) =
    runConverters(postProcessorModules, nodeToConvert, importStatus)


  private def runConverters(converters: Seq[ConverterModule], nodeToConvert: NodeToConvert, importStatus: ImportStatus): (NodeToConvert, ImportStatus) =
    converters.foldLeft((nodeToConvert, importStatus))((element, converter) => {
      val (updatedNodeToConvert, importStatus) = element
      converter.convert(updatedNodeToConvert, importStatus)
    })
}
