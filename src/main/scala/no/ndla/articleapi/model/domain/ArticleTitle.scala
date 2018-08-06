/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

case class ArticleTitle(title: String, language: String) extends LanguageField {
  override def isEmpty: Boolean = title.isEmpty
}
