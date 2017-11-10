/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

import java.util.Date

case class Copyright(license: String, origin: String, creators: Seq[Author], processors: Seq[Author], rightsholders: Seq[Author], agreement: Option[Long], validFrom: Option[Date], validTo: Option[Date])
