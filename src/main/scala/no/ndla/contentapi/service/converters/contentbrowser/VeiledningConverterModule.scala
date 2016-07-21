package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service.{ExtractConvertStoreContent, ExtractServiceComponent}

trait VeiledningConverterModule extends GeneralContentConverterModule {
  this: ExtractServiceComponent with ExtractConvertStoreContent with ContentRepositoryComponent =>

  object VeiledningConverter extends GeneralContentConverter {
    override val typeName: String = "veiledning"
  }
}
