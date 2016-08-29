/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import no.ndla.articleapi.integration.ConverterModule

trait ConverterModules {
  val converterModules: List[ConverterModule]
}
