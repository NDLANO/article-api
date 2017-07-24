/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.validation

import no.ndla.articleapi.ArticleApiProperties.{H5PResizerScriptUrl, NDLABrightcoveVideoScriptUrl, NRKVideoScriptUrl}
import no.ndla.articleapi.model.api.{ValidationException, ValidationMessage}
import no.ndla.articleapi.model.domain._
import no.ndla.mapping.ISO639.get6391CodeFor6392CodeMappings
import no.ndla.mapping.License.getLicense

import scala.util.{Failure, Success, Try}

trait ContentValidator {
  val contentValidator: ContentValidator
  val importValidator: ContentValidator

  class ContentValidator(allowEmptyLanguageField: Boolean) {
    private val NoHtmlValidator = new TextValidator(allowHtml=false)
    private val HtmlValidator = new TextValidator(allowHtml=true)

    def validate(content: Content): Try[Content] = {
      content match {
        case concept: Concept => validateConcept(concept)
        case article: Article => validateArticle(article)
      }
    }

    def validateArticle(article: Article): Try[Article] = {
      val validationErrors = article.content.flatMap(validateArticleContent) ++
        article.introduction.flatMap(validateIntroduction) ++
        article.metaDescription.flatMap(validateMetaDescription) ++
        article.title.flatMap(validateTitle) ++
        validateCopyright(article.copyright) ++
        validateTags(article.tags) ++
        article.requiredLibraries.flatMap(validateRequiredLibrary) ++
        article.metaImageId.flatMap(validateMetaImageId) ++
        article.visualElement.flatMap(validateVisualElement) ++
        validateArticleType(article.articleType)

      if (validationErrors.isEmpty) {
        Success(article)
      } else {
        Failure(new ValidationException(errors = validationErrors))
      }

    }

    private def validateConcept(concept: Concept): Try[Concept] = {
      val validationErrors = concept.content.flatMap(validateConceptContent) ++
        concept.title.flatMap(validateTitle)

      if (validationErrors.isEmpty) {
        Success(concept)
      } else {
        Failure(new ValidationException(errors = validationErrors))
      }
    }

    private def validateArticleType(articleType: String): Seq[ValidationMessage] = {
      ArticleType.valueOf(articleType) match {
        case None => Seq(ValidationMessage("articleType", s"$articleType is not a valid article type. Valid options are ${ArticleType.all.mkString(",")}"))
        case _ => Seq.empty
      }
    }

    private def validateArticleContent(content: ArticleContent): Seq[ValidationMessage] = {
      HtmlValidator.validate("content.content", content.content).toList ++
        validateLanguage("content.language", content.language)
    }

    private def validateConceptContent(content: ConceptContent): Seq[ValidationMessage] = {
      NoHtmlValidator.validate("content.content", content.content).toList ++
        validateLanguage("content.language", content.language)
    }

    private def validateVisualElement(content: VisualElement): Seq[ValidationMessage] = {
      HtmlValidator.validate("visualElement.content", content.resource).toList ++
        validateLanguage("visualElement.language", content.language)
    }

    private def validateIntroduction(content: ArticleIntroduction): Seq[ValidationMessage] = {
      NoHtmlValidator.validate("introduction.introduction", content.introduction).toList ++
        validateLanguage("introduction.language", content.language)
    }

    private def validateMetaDescription(content: ArticleMetaDescription): Seq[ValidationMessage] = {
      NoHtmlValidator.validate("metaDescription.metaDescription", content.content).toList ++
        validateLanguage("metaDescription.language", content.language)
    }

    private def validateTitle(content: LanguageField[String]): Seq[ValidationMessage] = {
      NoHtmlValidator.validate("title.title", content.value).toList ++
        validateLanguage("title.language", content.language)
    }

    private def validateCopyright(copyright: Copyright): Seq[ValidationMessage] = {
      val licenseMessage = validateLicense(copyright.license)
      val contributorsMessages = copyright.authors.flatMap(validateAuthor)
      val originMessage = NoHtmlValidator.validate("copyright.origin", copyright.origin)

      licenseMessage ++ contributorsMessages ++ originMessage
    }

    private def validateLicense(license: String): Seq[ValidationMessage] = {
      getLicense(license) match {
        case None => Seq(ValidationMessage("license.license", s"$license is not a valid license"))
        case _ => Seq()
      }
    }

    private def validateAuthor(author: Author): Seq[ValidationMessage] = {
      NoHtmlValidator.validate("author.type", author.`type`).toList ++
        NoHtmlValidator.validate("author.name", author.name).toList
    }

    private def validateTags(tags: Seq[ArticleTag]): Seq[ValidationMessage] = {
      tags.flatMap(tagList => {
        tagList.tags.flatMap(NoHtmlValidator.validate("tags.tags", _)).toList :::
          validateLanguage("tags.language", tagList.language).toList
      })
    }

    private def validateRequiredLibrary(requiredLibrary: RequiredLibrary): Option[ValidationMessage] = {
      val permittedLibraries = Seq(NDLABrightcoveVideoScriptUrl, H5PResizerScriptUrl) ++ NRKVideoScriptUrl
      permittedLibraries.contains(requiredLibrary.url) match {
        case false => Some(ValidationMessage("requiredLibraries.url", s"${requiredLibrary.url} is not a permitted script. Allowed scripts are: ${permittedLibraries.mkString(",")}"))
        case true => None
      }
    }

    private def validateMetaImageId(metaImageId: String): Option[ValidationMessage] = {
      def isAllDigits(x: String) = x forall Character.isDigit
      isAllDigits(metaImageId) match {
        case true => None
        case false => Some(ValidationMessage("metaImageId", "Meta image ID must be a number"))
      }
    }

    private def validateLanguage(fieldPath: String, languageCode: Option[String]): Option[ValidationMessage] = {
      if (languageCode.isEmpty && allowEmptyLanguageField)
        return None

      languageCode.flatMap(lang =>
        languageCodeSupported6391(lang) match {
          case true => None
          case false => Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
        })
    }

    private def languageCodeSupported6391(languageCode: String): Boolean =
      get6391CodeFor6392CodeMappings.exists(_._2 == languageCode)
  }
}
