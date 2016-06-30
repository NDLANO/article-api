package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.service.ExtractServiceComponent

trait AktualitetConverterModule extends GeneralContentConverterModule {
  this: ExtractServiceComponent =>

  object AktualitetConverter extends GeneralContentConverter {
    override val typeName: String = "aktualitet"
  }
}
