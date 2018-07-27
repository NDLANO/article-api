/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

case class ArticleIntroduction(introduction: String, language: String) extends LanguageField {
  override def isEmpty: Boolean = introduction.isEmpty
}

