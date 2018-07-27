/*
 * Part of NDLA article_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model.domain

case class ArticleMetaImage(imageId: String, altText: String, language: String) extends LanguageField {
  override def isEmpty: Boolean = imageId.isEmpty && altText.isEmpty
}
