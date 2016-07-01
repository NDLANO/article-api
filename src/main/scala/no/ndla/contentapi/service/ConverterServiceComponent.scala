package no.ndla.contentapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.ContentApiProperties.maxConvertionRounds
import no.ndla.contentapi.integration.NodeToConvert
import no.ndla.contentapi.model.{ContentInformation, ImportStatus}

import scala.annotation.tailrec

trait ConverterServiceComponent {
  this: ConverterModules =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    def convertNode(nodeToConvert: NodeToConvert): (ContentInformation, ImportStatus) = {
      @tailrec def convertNode(nodeToConvert: NodeToConvert, maxRoundsLeft: Int, importStatus: ImportStatus = ImportStatus()): (ContentInformation, ImportStatus) = {
        if (maxRoundsLeft == 0) {
          val message = "Maximum number of converter rounds reached; Some content might not be converted"
          logger.warn(message)
          return (nodeToConvert.asContentInformation, ImportStatus(importStatus.messages :+ message))
        }

        val (updatedContent, updatedStatus) = convert(nodeToConvert)

        // If this converting round did not yield any changes to the content, this node is finished (case true)
        // If changes were made during this convertion, we run the converters again (case false)
        updatedContent == nodeToConvert match {
          case true => (updatedContent.asContentInformation, ImportStatus(importStatus.messages ++ updatedStatus.messages))
          case false => convertNode(updatedContent, maxRoundsLeft - 1, ImportStatus(importStatus.messages ++ updatedStatus.messages))
        }
      }

      convertNode(nodeToConvert, maxConvertionRounds)
    }

    private def convert(nodeToConvert: NodeToConvert): (NodeToConvert, ImportStatus) =
      converterModules.foldLeft((nodeToConvert, ImportStatus()))((element, converter) => {
        val (updatedNodeToConvert, importStatus) = element
        converter.convert(updatedNodeToConvert, importStatus)
      })
  }
}
