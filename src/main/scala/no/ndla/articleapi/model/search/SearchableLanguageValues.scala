/*
 * Part of NDLA article_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model.search

import no.ndla.articleapi.model.domain.LanguageField

case class LanguageValue[T](language: String, value: T) extends LanguageField {
  override def isEmpty: Boolean = false
}

case class SearchableLanguageValues(languageValues: Seq[LanguageValue[String]])

case class SearchableLanguageList(languageValues: Seq[LanguageValue[Seq[String]]])