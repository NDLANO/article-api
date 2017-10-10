/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.model.domain

case class ImportStatus(messages: Seq[String], visitedNodes: Set[String] = Set(), articleId: Option[Long] = None) {
  def ++(importStatus: ImportStatus): ImportStatus =
    ImportStatus(messages ++ importStatus.messages, visitedNodes ++ importStatus.visitedNodes, importStatus.articleId)
  def addMessage(message: String): ImportStatus = this.copy(messages = this.messages :+ message)
  def addMessages(messages: Seq[String]): ImportStatus = this.copy(messages = this.messages ++ messages)
  def addVisitedNode(nodeID: String): ImportStatus = this.copy(visitedNodes = this.visitedNodes + nodeID)
  def addVisitedNodes(nodeIDs: Set[String]): ImportStatus = this.copy(visitedNodes = this.visitedNodes ++ nodeIDs)
  def setArticleId(id: Long): ImportStatus = this.copy(articleId = Some(id))
}

object ImportStatus {
  def empty = ImportStatus(Seq.empty, Set.empty, None)
  def apply(message: String, visitedNodes: Set[String]): ImportStatus = ImportStatus(Seq(message), visitedNodes, None)
  def apply(importStatuses: Seq[ImportStatus]): ImportStatus = {
    val (messages, visitedNodes, articleIds) = importStatuses.map(x => (x.messages, x.visitedNodes, x.articleId)).unzip3
    ImportStatus(messages.flatten.distinct, visitedNodes.flatten.toSet, articleIds.lastOption.flatten)
  }

}
