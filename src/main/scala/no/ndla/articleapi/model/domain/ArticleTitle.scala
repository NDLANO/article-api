/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

case class ArticleTitle(title: String, language: Option[String]) extends LanguageField[String] {
  override def value: String = title
}
