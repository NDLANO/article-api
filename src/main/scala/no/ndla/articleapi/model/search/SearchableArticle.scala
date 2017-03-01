/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.search

import java.util.Date

import no.ndla.articleapi.model.domain.emptySomeToNone
import no.ndla.articleapi.model.search.LanguageValue.LanguageValue


object LanguageValue {

  case class LanguageValue[T](lang: Option[String], value: T)

  def apply[T](lang: Option[String], value: T): LanguageValue[T] = LanguageValue(emptySomeToNone(lang), value)

}

case class SearchableLanguageValues(languageValues: Seq[LanguageValue[String]])

case class SearchableLanguageList(languageValues: Seq[LanguageValue[Seq[String]]])

case class SearchableArticle(
  id: Long,
  title: SearchableLanguageValues,
  content: SearchableLanguageValues,
  visualElement: SearchableLanguageValues,
  introduction: SearchableLanguageValues,
  tags: SearchableLanguageList,
  lastUpdated: Date,
  license: String,
  authors: Seq[String]
)
