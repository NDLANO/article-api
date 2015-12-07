package no.ndla.contentapi.business

import no.ndla.contentapi.model.{ContentSummary}

trait ContentSearch {
  def all(license: Option[String]): Iterable[ContentSummary]
  def matchingQuery(query: Iterable[String], language: Option[String], license: Option[String]): Iterable[ContentSummary]
}
