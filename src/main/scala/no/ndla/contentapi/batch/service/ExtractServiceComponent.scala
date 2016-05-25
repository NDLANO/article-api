package no.ndla.contentapi.batch.service

import no.ndla.contentapi.batch.integration.CMDataComponent
import no.ndla.contentapi.model.ContentInformation

trait ExtractServiceComponent {
  this: CMDataComponent =>

  val extractorService: ExtractService

  class ExtractService {
    def importNode(nodeId: String): ContentInformation = cmData.getNode(nodeId)
  }
}
