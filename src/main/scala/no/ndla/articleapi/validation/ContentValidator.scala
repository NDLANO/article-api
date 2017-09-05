/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.validation

import no.ndla.articleapi.ArticleApiProperties.{H5PResizerScriptUrl, NDLABrightcoveVideoScriptUrl, NRKVideoScriptUrl}
import no.ndla.articleapi.integration.ConverterModule.stringToJsoupDocument
import no.ndla.articleapi.model.api.{ValidationException, ValidationMessage}
import no.ndla.articleapi.model.domain._
import no.ndla.mapping.ISO639.get6391CodeFor6392CodeMappings
import no.ndla.mapping.License.getLicense

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

trait ContentValidator {
  val contentValidator: ContentValidator
  val importValidator: ContentValidator

  class ContentValidator(allowEmptyLanguageField: Boolean) {
    private val NoHtmlValidator = new TextValidator(allowHtml=false)
    private val HtmlValidator = new TextValidator(allowHtml=true)

    def validate(content: Content, allowUnknownLanguage: Boolean = false): Try[Content] = {
      content match {
        case concept: Concept => validateConcept(concept, allowUnknownLanguage)
        case article: Article => validateArticle(article, allowUnknownLanguage)
      }
    }

    def validateArticle(article: Article, allowUnknownLanguage: Boolean): Try[Article] = {
      val validationErrors = article.content.flatMap(c => validateArticleContent(c, allowUnknownLanguage)) ++
        article.introduction.flatMap(i => validateIntroduction(i, allowUnknownLanguage)) ++
        article.metaDescription.flatMap(m => validateMetaDescription(m, allowUnknownLanguage)) ++
        article.title.flatMap(t => validateTitle(t, allowUnknownLanguage)) ++
        validateCopyright(article.copyright, allowUnknownLanguage) ++
        validateTags(article.tags, allowUnknownLanguage) ++
        article.requiredLibraries.flatMap(validateRequiredLibrary) ++
        article.metaImageId.flatMap(validateMetaImageId) ++
        article.visualElement.flatMap(v => validateVisualElement(v, allowUnknownLanguage)) ++
        validateArticleType(article.articleType)

      if (validationErrors.isEmpty) {
        Success(article)
      } else {
        Failure(new ValidationException(errors = validationErrors))
      }

    }

    private def validateConcept(concept: Concept, allowUnknownLanguage: Boolean): Try[Concept] = {
      val validationErrors = concept.content.flatMap(c => validateConceptContent(c, allowUnknownLanguage)) ++
        concept.title.flatMap(t => validateTitle(t, allowUnknownLanguage))

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

    private def validateArticleContent(content: ArticleContent, allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      HtmlValidator.validate("content.content", content.content).toList ++
        rootElementContainsOnlySectionBlocks("content.content", content.content) ++
        validateLanguage("content.language", content.language, allowUnknownLanguage)
    }

    def rootElementContainsOnlySectionBlocks(field: String, html: String): Option[ValidationMessage] = {
      val legalTopLevelTag = "section"
      val topLevelTags = stringToJsoupDocument(html).children().asScala.map(_.tagName())

      topLevelTags.forall(_ == legalTopLevelTag) match {
        case true => None
        case false =>
          val illegalTags = topLevelTags.filterNot(_ == legalTopLevelTag).mkString(",")
          Some(ValidationMessage(field, s"An article must consist of one or more <section> blocks. Illegal tag(s) are $illegalTags "))
      }
    }

    private def validateConceptContent(content: ConceptContent, allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      NoHtmlValidator.validate("content.content", content.content).toList ++
        validateLanguage("content.language", content.language, allowUnknownLanguage)
    }

    private def validateVisualElement(content: VisualElement, allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      HtmlValidator.validate("visualElement.content", content.resource).toList ++
        validateLanguage("visualElement.language", content.language, allowUnknownLanguage)
    }

    private def validateIntroduction(content: ArticleIntroduction, allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      NoHtmlValidator.validate("introduction.introduction", content.introduction).toList ++
        validateLanguage("introduction.language", content.language, allowUnknownLanguage)
    }

    private def validateMetaDescription(content: ArticleMetaDescription, allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      NoHtmlValidator.validate("metaDescription.metaDescription", content.content).toList ++
        validateLanguage("metaDescription.language", content.language, allowUnknownLanguage)
    }

    private def validateTitle(content: LanguageField[String], allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      NoHtmlValidator.validate("title.title", content.value).toList ++
        validateLanguage("title.language", content.language, allowUnknownLanguage)
    }

    private def validateCopyright(copyright: Copyright, allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
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

    private def validateTags(tags: Seq[ArticleTag], allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      tags.flatMap(tagList => {
        tagList.tags.flatMap(NoHtmlValidator.validate("tags.tags", _)).toList :::
          validateLanguage("tags.language", tagList.language, allowUnknownLanguage).toList
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

    private def validateLanguage(fieldPath: String, languageCode: String, allowUnknownLanguage: Boolean): Option[ValidationMessage] = {
      languageCode.nonEmpty && languageCodeSupported6391(languageCode, allowUnknownLanguage) match {
        case true => None
        case false => Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
      }
    }

    private def languageCodeSupported6391(languageCode: String, allowUnknownLanguage: Boolean): Boolean = {
      val languageCodes = get6391CodeFor6392CodeMappings.values.toSeq ++ (if (allowUnknownLanguage) Seq("unknown") else Seq.empty)
      languageCodes.contains(languageCode)
    }
  }
}
