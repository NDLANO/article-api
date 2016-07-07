package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.service.ExtractServiceComponent

trait VeiledningConverterModule extends GeneralContentConverterModule {
  this: ExtractServiceComponent =>

  object VeiledningConverter extends GeneralContentConverter {
    override val typeName: String = "veiledning"
  }
}
