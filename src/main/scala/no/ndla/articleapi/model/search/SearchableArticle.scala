/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.search

import org.joda.time.DateTime

case class SearchableArticle(
  id: Long,
  title: SearchableLanguageValues,
  content: SearchableLanguageValues,
  visualElement: SearchableLanguageValues,
  introduction: SearchableLanguageValues,
  metaDescription: SearchableLanguageValues,
  metaImage: SearchableLanguageValues,
  tags: SearchableLanguageList,
  lastUpdated: DateTime,
  license: String,
  authors: Seq[String],
  articleType: String,
  defaultTitle: Option[String]
)

case class SearchableConcept(
  id: Long,
  title: SearchableLanguageValues,
  content: SearchableLanguageValues,
  defaultTitle: Option[String]
)
