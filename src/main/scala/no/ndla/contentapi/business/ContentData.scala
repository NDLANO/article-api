package no.ndla.contentapi.business

import no.ndla.contentapi.model.ContentInformation

trait ContentData {
  def withId(contentId: String): Option[ContentInformation]
  def insert(contentInformation: ContentInformation, externalId: String): Unit
}
