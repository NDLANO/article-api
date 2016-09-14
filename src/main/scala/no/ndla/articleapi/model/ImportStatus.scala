/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.model

case class ImportStatus(messages: Seq[String], visitedNodes: Seq[String] = Seq()) {
  def ++(importStatus: ImportStatus): ImportStatus =
    ImportStatus(messages ++ importStatus.messages, visitedNodes ++ importStatus.visitedNodes)
}
object ImportStatus {
  def apply(message: String, visitedNodes: Seq[String]): ImportStatus = ImportStatus(Seq(message), visitedNodes)
  def apply(importStatuses: Seq[ImportStatus]): ImportStatus = {
    val (messages, visitedNodes) = importStatuses.map(x => (x.messages, x.visitedNodes)).unzip
    ImportStatus(messages.flatten.distinct, visitedNodes.flatten.distinct)
  }

}
