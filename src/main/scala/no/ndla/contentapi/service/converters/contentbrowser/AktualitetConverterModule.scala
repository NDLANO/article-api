package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service.{ExtractConvertStoreContent, ExtractServiceComponent}

trait AktualitetConverterModule extends GeneralContentConverterModule {
  this: ExtractServiceComponent with ExtractConvertStoreContent with ContentRepositoryComponent =>

  object AktualitetConverter extends GeneralContentConverter {
    override val typeName: String = "aktualitet"
  }
}
