/*
 * Part of NDLA article_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model.domain

case class ArticleMetaImage(imageId: String, language: String) extends LanguageField[String] {
  override def value: String = imageId
}
