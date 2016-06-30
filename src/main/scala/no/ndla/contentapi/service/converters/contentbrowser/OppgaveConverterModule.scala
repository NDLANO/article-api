package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.service.ExtractServiceComponent

trait OppgaveConverterModule extends GeneralContentConverterModule {
  this: ExtractServiceComponent =>

  object OppgaveConverter extends GeneralContentConverter {
    override val typeName: String = "oppgave"
  }
}