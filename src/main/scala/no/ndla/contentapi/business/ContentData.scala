package no.ndla.contentapi.business

import no.ndla.contentapi.model.{ContentSummary, ContentInformation}

trait ContentData {
  def withId(contentId: String): Option[ContentInformation]
  def all(): List[ContentSummary]
  def insert(contentInformation: ContentInformation, externalId: String): Unit
}
