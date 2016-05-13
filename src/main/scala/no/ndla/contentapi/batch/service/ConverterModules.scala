package no.ndla.contentapi.batch.service

import no.ndla.contentapi.batch.integration.ConverterModule

trait ConverterModules {
  val converterModules: List[ConverterModule]
}
