/*
 * Part of NDLA article-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.repository

import no.ndla.articleapi.model.domain.Content
import scalikejdbc.{AutoSession, DBSession}

trait Repository[T <: Content] {
  def minMaxId(implicit session: DBSession = AutoSession): (Long, Long)
  def documentsWithIdBetween(min: Long, max: Long): Seq[T]
}
