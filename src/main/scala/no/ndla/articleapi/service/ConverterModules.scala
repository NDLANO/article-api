package no.ndla.articleapi.service

import no.ndla.articleapi.integration.ConverterModule

trait ConverterModules {
  val converterModules: List[ConverterModule]
}
