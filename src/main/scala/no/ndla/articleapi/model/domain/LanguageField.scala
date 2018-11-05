package no.ndla.articleapi.model.domain

trait LanguageField[T] {
  def value: T
  def language: String
  def isEmpty: Boolean
}
