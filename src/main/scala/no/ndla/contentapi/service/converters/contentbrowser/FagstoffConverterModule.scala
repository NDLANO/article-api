package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service.{ExtractConvertStoreContent, ExtractServiceComponent}

trait FagstoffConverterModule extends GeneralContentConverterModule {
  this: ExtractServiceComponent with ExtractConvertStoreContent with ContentRepositoryComponent =>

  object FagstoffConverter extends GeneralContentConverter {
    override val typeName: String = "fagstoff"
  }
}
