/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.model.domain

case class VisualElement(resource: String, language: String) extends LanguageField {
  override def isEmpty: Boolean = resource.isEmpty
}
