package no.ndla.articleapi.model.domain

trait WithLanguage {
  def language: String
}

trait LanguageField[T] extends WithLanguage {
  def value: T
  def language: String
}
