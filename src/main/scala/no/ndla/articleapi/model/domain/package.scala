/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.articleapi.model

package object domain {

  def emptySomeToNone(lang: Option[String]): Option[String] = {
    lang match {
      case Some(s) if s.isEmpty => None
      case Some(s) => Some(s)
      case None => None
    }
  }

}
