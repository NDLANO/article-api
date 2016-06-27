package no.ndla.contentapi.service

import no.ndla.contentapi.ContentApiProperties.maxConvertionRounds
import no.ndla.contentapi.integration.NodeToConvert
import no.ndla.contentapi.model.{ContentInformation, ImportStatus}

import scala.annotation.tailrec

trait ConverterServiceComponent {
  this: ConverterModules =>
  val converterService: ConverterService

  class ConverterService {
    def convertNode(contentInformation: NodeToConvert): (ContentInformation, ImportStatus) = {
      @tailrec def convertNode(contentInformation: NodeToConvert, maxRoundsLeft: Int, importStatus: ImportStatus = ImportStatus()): (ContentInformation, ImportStatus) = {
        if (maxRoundsLeft == 0)
          return (contentInformation.asContentInformation, importStatus)

        val (updatedContent, updatedStatus) = convert(contentInformation)

        // If this converting round did not yield any changes to the content, this node is finished (case true)
        // If changes were made during this convertion, we run the converters again (case false)
        updatedContent == contentInformation match {
          case true => (updatedContent.asContentInformation, ImportStatus(importStatus.messages ++ updatedStatus.messages))
          case false => convertNode(updatedContent, maxRoundsLeft - 1, ImportStatus(importStatus.messages ++ updatedStatus.messages))
        }
      }

      convertNode(contentInformation, maxConvertionRounds)
    }

    private def convert(contentInformation: NodeToConvert): (NodeToConvert, ImportStatus) =
      converterModules.foldLeft((contentInformation, ImportStatus()))((element, converter) => {
        val (contentInformation, importStatus) = element
        converter.convert(contentInformation, importStatus)
      })
  }
}
