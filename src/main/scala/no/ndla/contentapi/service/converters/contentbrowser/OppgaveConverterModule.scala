package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service.{ConverterServiceComponent, ExtractConvertStoreContent, ExtractServiceComponent}

trait OppgaveConverterModule extends GeneralContentConverterModule {
  this: ExtractServiceComponent with ExtractConvertStoreContent with ConverterServiceComponent with ContentRepositoryComponent =>

  object OppgaveConverter extends GeneralContentConverter {
    override val typeName: String = "oppgave"
  }
}
