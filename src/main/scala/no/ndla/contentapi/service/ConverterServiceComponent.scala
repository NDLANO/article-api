package no.ndla.contentapi.service

import no.ndla.contentapi.model.{ContentInformation, ImportStatus}

trait ConverterServiceComponent {
  this: ConverterModules =>
  val converterService: ConverterService

  class ConverterService {
    def convertNode(contentInformation: ContentInformation): (ContentInformation, ImportStatus) =
      converterModules.foldLeft((contentInformation, ImportStatus(List[String]())))((element, converter) => {
        converter.convert(element._1) match { case (content, status) => (content, element._2.join(status)) }
      })
  }
}
