/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

case class ConceptContent(content: String, language: String) extends LanguageField[String] {
  override def value: String = content
}
