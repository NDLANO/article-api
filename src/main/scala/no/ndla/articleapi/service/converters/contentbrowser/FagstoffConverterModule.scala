package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.repository.ContentRepositoryComponent
import no.ndla.articleapi.service.{ExtractConvertStoreContent, ExtractServiceComponent}

trait FagstoffConverterModule extends GeneralContentConverterModule {
  this: ExtractServiceComponent with ExtractConvertStoreContent with ContentRepositoryComponent =>

  object FagstoffConverter extends GeneralContentConverter {
    override val typeName: String = "fagstoff"
  }
}
