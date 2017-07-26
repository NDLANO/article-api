/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.service.converters.HtmlTagGenerator
import no.ndla.articleapi.service.{ConverterService, ExtractConvertStoreContent, ExtractService, ReadService}

trait OppgaveConverterModule extends GeneralContentConverterModule {
  this: ExtractService with ExtractConvertStoreContent with ConverterService with ReadService with HtmlTagGenerator =>

  object OppgaveConverter extends GeneralContentConverter {
    override val typeName: String = "oppgave"
  }
}
