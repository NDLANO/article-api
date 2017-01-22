package no.ndla.articleapi.model.domain

trait LanguageField {
  def value: String
  def language: Option[String]
}
