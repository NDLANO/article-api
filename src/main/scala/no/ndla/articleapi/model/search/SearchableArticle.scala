/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.search

case class SearchableArticle(nb: Option[String],
                             nn: Option[String],
                             en: Option[String],
                             fr: Option[String],
                             de: Option[String],
                             es: Option[String],
                             se: Option[String],
                             zh: Option[String],
                             unknown: Option[String])
