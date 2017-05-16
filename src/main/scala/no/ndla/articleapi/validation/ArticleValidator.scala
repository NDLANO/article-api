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

trait ArticleValidator {
  val articleValidator: ArticleValidator

  class ArticleValidator {
    private val NoHtmlValidator = new TextValidator(allowHtml=false)
    private val HtmlValidator = new TextValidator(allowHtml=true)

    def validateArticle(article: Article) = {
      val validationErrors = article.content.flatMap(validateContent) ++
        article.introduction.flatMap(validateIntroduction) ++
        article.metaDescription.flatMap(validateMetaDescription) ++
        article.title.flatMap(validateTitle) ++
        validateCopyright(article.copyright) ++
        validateTags(article.tags) ++
        article.requiredLibraries.flatMap(validateRequiredLibrary) ++
        article.metaImageId.flatMap(validateMetaImageId) ++
        article.visualElement.flatMap(validateVisualElement) ++
        validateArticleType(article.articleType)

      if (validationErrors.nonEmpty)
        throw new ValidationException(errors=validationErrors)
    }

    def validateArticleType(articleType: String): Seq[ValidationMessage] = {
      ArticleType.valueOf(articleType) match {
        case None => Seq(ValidationMessage("articleType", s"$articleType is not a valid article type. Valid options are ${ArticleType.all.mkString(",")}"))
        case _ => Seq.empty
      }
    }

    def validateContent(content: ArticleContent): Seq[ValidationMessage] = {
      HtmlValidator.validate("content.content", content.content).toList ++
        validateLanguage("content.language", content.language)
    }

    def validateVisualElement(content: VisualElement): Seq[ValidationMessage] = {
      HtmlValidator.validate("visualElement.content", content.resource).toList ++
        validateLanguage("visualElement.language", content.language)
    }

    def validateIntroduction(content: ArticleIntroduction): Seq[ValidationMessage] = {
      NoHtmlValidator.validate("introduction.introduction", content.introduction).toList ++
        validateLanguage("introduction.language", content.language)
    }

    def validateMetaDescription(content: ArticleMetaDescription): Seq[ValidationMessage] = {
      NoHtmlValidator.validate("metaDescription.metaDescription", content.content).toList ++
        validateLanguage("metaDescription.language", content.language)
    }

    def validateTitle(content: ArticleTitle): Seq[ValidationMessage] = {
      NoHtmlValidator.validate("title.title", content.title).toList ++
        validateLanguage("title.language", content.language)
    }

    def validateCopyright(copyright: Copyright): Seq[ValidationMessage] = {
      val licenseMessage = validateLicense(copyright.license)
      val contributorsMessages = copyright.authors.flatMap(validateAuthor)
      val originMessage = NoHtmlValidator.validate("copyright.origin", copyright.origin)

      licenseMessage ++ contributorsMessages ++ originMessage
    }

    def validateLicense(license: String): Seq[ValidationMessage] = {
      getLicense(license) match {
        case None => Seq(ValidationMessage("license.license", s"$license is not a valid license"))
        case _ => Seq()
      }
    }

    def validateAuthor(author: Author): Seq[ValidationMessage] = {
      NoHtmlValidator.validate("author.type", author.`type`).toList ++
        NoHtmlValidator.validate("author.name", author.name).toList
    }

    def validateTags(tags: Seq[ArticleTag]): Seq[ValidationMessage] = {
      tags.flatMap(tagList => {
        tagList.tags.flatMap(NoHtmlValidator.validate("tags.tags", _)).toList :::
          validateLanguage("tags.language", tagList.language).toList
      })
    }

    def validateRequiredLibrary(requiredLibrary: RequiredLibrary): Option[ValidationMessage] = {
      val permittedLibraries = Seq(NDLABrightcoveVideoScriptUrl, H5PResizerScriptUrl, NRKVideoScriptUrl) // TODO find better way of generating this list. (avoid hardcoding list?)
      permittedLibraries.contains(requiredLibrary.url) match {
        case false => Some(ValidationMessage("requiredLibraries.url", s"${requiredLibrary.url} is not a permitted script. Allowed scripts are: ${permittedLibraries.mkString(",")}"))
        case true => None
      }
    }

    def validateMetaImageId(metaImageId: String): Option[ValidationMessage] = {
      def isAllDigits(x: String) = x forall Character.isDigit
      isAllDigits(metaImageId) match {
        case true => None
        case false => Some(ValidationMessage("metaImageId", "Meta image ID must be a number"))
      }
    }

    private def validateLanguage(fieldPath: String, languageCode: Option[String]): Option[ValidationMessage] = {
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
