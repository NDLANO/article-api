package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.service.ExtractServiceComponent

trait FagstoffConverterModule extends GeneralContentConverterModule {
  this: ExtractServiceComponent =>

  object FagstoffConverter extends GeneralContentConverter {
    override val typeName: String = "fagstoff"
  }
}
