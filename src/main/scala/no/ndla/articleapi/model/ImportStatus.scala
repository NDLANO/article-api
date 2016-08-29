/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.model

case class ImportStatus(messages: Seq[String], visitedNodes: Seq[String] = Seq())
object ImportStatus {
  def apply(message: String, visitedNodes: Seq[String]): ImportStatus = ImportStatus(Seq(message), visitedNodes)
}
