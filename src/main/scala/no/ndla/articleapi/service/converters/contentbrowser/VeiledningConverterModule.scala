package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.repository.ContentRepositoryComponent
import no.ndla.articleapi.service.{ExtractConvertStoreContent, ExtractServiceComponent}

trait VeiledningConverterModule extends GeneralContentConverterModule {
  this: ExtractServiceComponent with ExtractConvertStoreContent with ContentRepositoryComponent =>

  object VeiledningConverter extends GeneralContentConverter {
    override val typeName: String = "veiledning"
  }
}
