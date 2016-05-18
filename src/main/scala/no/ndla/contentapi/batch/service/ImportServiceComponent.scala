package no.ndla.contentapi.batch.service

import no.ndla.contentapi.batch.integration.CMDataComponent
import no.ndla.contentapi.model.ContentInformation

trait ImportServiceComponent {
  this: CMDataComponent =>

  val importService: ImportService

  class ImportService {
    def importNode(nodeId: String): ContentInformation = cmData.getNode(nodeId)
  }
}
