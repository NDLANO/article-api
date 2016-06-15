package no.ndla.contentapi.service

import no.ndla.contentapi.model.{ContentInformation, ImportStatus}

trait ConverterServiceComponent {
  this: ConverterModules =>
  val converterService: ConverterService

  class ConverterService {
    def convertNode(contentInformation: ContentInformation): (ContentInformation, ImportStatus) =
      converterModules.foldLeft((contentInformation, ImportStatus()))((element, converter) => {
        val (contentInformation, importStatus) = element
        converter.convert(contentInformation, importStatus)
      })
  }
}
