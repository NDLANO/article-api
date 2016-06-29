package no.ndla.contentapi.service

import no.ndla.contentapi.ContentApiProperties.maxConvertionRounds
import no.ndla.contentapi.model.{ContentInformation, ImportStatus}
import scala.annotation.tailrec

trait ConverterServiceComponent {
  this: ConverterModules =>
  val converterService: ConverterService

  class ConverterService {
    def convertNode(contentInformation: ContentInformation): (ContentInformation, ImportStatus) = {
      @tailrec def convertNode(contentInformation: ContentInformation, maxRoundsLeft: Int, importStatus: ImportStatus = ImportStatus()): (ContentInformation, ImportStatus) = {
        if (maxRoundsLeft == 0) {
          val message = "Maximum number of converter rounds reached; Some content might not be converted"
          return (contentInformation, ImportStatus(importStatus.messages :+ message))
        }

        val (updatedContent, updatedStatus) = convert(contentInformation)

        // If this converting round did not yield any changes to the content, this node is finished (case true)
        // If changes were made during this convertion, we run the converters again (case false)
        updatedContent == contentInformation match {
          case true => (updatedContent, updatedStatus)
          case false => convertNode(updatedContent, maxRoundsLeft - 1, ImportStatus(importStatus.messages ++ updatedStatus.messages))
        }
      }

      convertNode(contentInformation, maxConvertionRounds)
    }

    private def convert(contentInformation: ContentInformation): (ContentInformation, ImportStatus) =
      converterModules.foldLeft((contentInformation, ImportStatus()))((element, converter) => {
        val (contentInformation, importStatus) = element
        converter.convert(contentInformation, importStatus)
      })
  }
}
