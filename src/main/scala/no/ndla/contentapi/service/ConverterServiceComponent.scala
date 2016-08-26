package no.ndla.contentapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.ContentApiProperties.maxConvertionRounds
import no.ndla.contentapi.model.{ContentInformation, ImportStatus, NodeToConvert}

import scala.annotation.tailrec

trait ConverterServiceComponent {
  this: ConverterModules =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    def convertNode(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (ContentInformation, ImportStatus) = {
      @tailrec def convertNode(nodeToConvert: NodeToConvert, maxRoundsLeft: Int, importStatus: ImportStatus): (ContentInformation, ImportStatus) = {
        if (maxRoundsLeft == 0) {
          val message = "Maximum number of converter rounds reached; Some content might not be converted"
          logger.warn(message)
          return (nodeToConvert.asContentInformation, importStatus.copy(messages=importStatus.messages :+ message))
        }

        val (updatedContent, updatedStatus) = convert(nodeToConvert, importStatus)

        // If this converting round did not yield any changes to the content, this node is finished (case true)
        // If changes were made during this convertion, we run the converters again (case false)
        updatedContent == nodeToConvert match {
          case true => (updatedContent.asContentInformation, updatedStatus)
          case false => convertNode(updatedContent, maxRoundsLeft - 1, updatedStatus)
        }
      }

      val updatedVisitedNodes = importStatus.visitedNodes ++ nodeToConvert.contents.map(_.nid)
      convertNode(nodeToConvert, maxConvertionRounds, importStatus.copy(visitedNodes = updatedVisitedNodes.distinct))
    }

    private def convert(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (NodeToConvert, ImportStatus) =
      converterModules.foldLeft((nodeToConvert, importStatus))((element, converter) => {
        val (updatedNodeToConvert, importStatus) = element
        converter.convert(updatedNodeToConvert, importStatus)
      })
  }
}
