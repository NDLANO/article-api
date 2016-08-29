/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.repository.ContentRepositoryComponent
import no.ndla.articleapi.service.{ConverterServiceComponent, ExtractConvertStoreContent, ExtractServiceComponent}

trait OppgaveConverterModule extends GeneralContentConverterModule {
  this: ExtractServiceComponent with ExtractConvertStoreContent with ConverterServiceComponent with ContentRepositoryComponent =>

  object OppgaveConverter extends GeneralContentConverter {
    override val typeName: String = "oppgave"
  }
}
