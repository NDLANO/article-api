package no.ndla.contentapi.business

import no.ndla.contentapi.model.ContentInformation

trait ContentIndex {
  def indexDocuments(docs: List[ContentInformation], indexName: String): Int
  def create():String
  def delete(indexName: String): Unit
  def aliasTarget:Option[String]
  def updateAliasTarget(oldIndexName:Option[String], indexName: String)
}
