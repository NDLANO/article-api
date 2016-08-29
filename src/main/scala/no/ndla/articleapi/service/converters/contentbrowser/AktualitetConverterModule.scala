package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.repository.ContentRepositoryComponent
import no.ndla.articleapi.service.{ExtractConvertStoreContent, ExtractServiceComponent}

trait AktualitetConverterModule extends GeneralContentConverterModule {
  this: ExtractServiceComponent with ExtractConvertStoreContent with ContentRepositoryComponent =>

  object AktualitetConverter extends GeneralContentConverter {
    override val typeName: String = "aktualitet"
  }
}
