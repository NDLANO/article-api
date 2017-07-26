/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.service.converters.HtmlTagGenerator
import no.ndla.articleapi.service.{ExtractConvertStoreContent, ExtractService, ReadService}

trait FagstoffConverterModule extends GeneralContentConverterModule {
  this: ExtractService with ExtractConvertStoreContent with ReadService with HtmlTagGenerator =>

  object FagstoffConverter extends GeneralContentConverter {
    override val typeName: String = "fagstoff"
  }
}
