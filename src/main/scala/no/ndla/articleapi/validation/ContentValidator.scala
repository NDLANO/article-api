/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.validation

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.ArticleApiProperties.{H5PResizerScriptUrl, NDLABrightcoveVideoScriptUrl, NRKVideoScriptUrl}
import no.ndla.articleapi.integration.ConverterModule.stringToJsoupDocument
import no.ndla.articleapi.integration.DraftApiClient
import no.ndla.articleapi.model.domain._
import no.ndla.mapping.ISO639.get6391CodeFor6392CodeMappings
import no.ndla.mapping.License.getLicense
import no.ndla.validation.{TextValidator, ValidationException, ValidationMessage}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

trait ContentValidator {
  this: DraftApiClient =>
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
        validateCopyright(article.copyright) ++
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
      val field = s"content.${content.language}"
      HtmlValidator.validate(field, content.content).toList ++
        rootElementContainsOnlySectionBlocks(field, content.content) ++
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
      val field = s"content.${content.language}"
      NoHtmlValidator.validate(field, content.content).toList ++
        validateLanguage("content.language", content.language, allowUnknownLanguage)
    }

    private def validateVisualElement(content: VisualElement, allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      val field = s"visualElement.${content.language}"
      HtmlValidator.validate(field, content.resource).toList ++
        validateLanguage("visualElement.language", content.language, allowUnknownLanguage)
    }

    private def validateIntroduction(content: ArticleIntroduction, allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      val field = s"introduction.${content.language}"
      NoHtmlValidator.validate(field, content.introduction).toList ++
        validateLanguage("introduction.language", content.language, allowUnknownLanguage)
    }

    private def validateMetaDescription(content: ArticleMetaDescription, allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      val field = s"metaDescription.${content.language}"
      NoHtmlValidator.validate(field, content.content).toList ++
        validateLanguage("metaDescription.language", content.language, allowUnknownLanguage)
    }

    private def validateTitle(content: LanguageField[String], allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      val field = s"title.${content.language}"
      NoHtmlValidator.validate(field, content.value).toList ++
        validateLanguage("title.language", content.language, allowUnknownLanguage)
    }

    private def validateCopyright(copyright: Copyright): Seq[ValidationMessage] = {
      val licenseMessage = validateLicense(copyright.license)
      val contributorsMessages =
        copyright.creators.flatMap(a => validateAuthor(a, "copyright.creators", ArticleApiProperties.creatorTypes)) ++
        copyright.processors.flatMap(a => validateAuthor(a, "copyright.processors", ArticleApiProperties.processorTypes)) ++
        copyright.rightsholders.flatMap(a => validateAuthor(a, "copyright.rightsholders", ArticleApiProperties.rightsholderTypes))
      val originMessage = NoHtmlValidator.validate("copyright.origin", copyright.origin)
      val agreementMessage = validateAgreement(copyright)

      licenseMessage ++ contributorsMessages ++ originMessage ++ agreementMessage
    }

    def validateAgreement(copyright: Copyright): Seq[ValidationMessage] = {
      copyright.agreementId match {
        case Some(id) =>
          draftApiClient.agreementExists(id) match {
            case false => Seq (ValidationMessage ("copyright.agreement", s"Agreement with id $id does not exist") )
            case _ => Seq()
          }
        case _ => Seq()
      }
    }

    private def validateLicense(license: String): Seq[ValidationMessage] = {
      getLicense(license) match {
        case None => Seq(ValidationMessage("license.license", s"$license is not a valid license"))
        case _ => Seq()
      }
    }

    private def validateAuthor(author: Author, fieldPath: String, allowedTypes: Seq[String]): Seq[ValidationMessage] = {
      NoHtmlValidator.validate(s"$fieldPath.type", author.`type`).toList ++
        NoHtmlValidator.validate(s"$fieldPath.name", author.name).toList ++
        validateAuthorType(s"$fieldPath.type", author.`type`, allowedTypes).toList
    }

    def validateAuthorType(fieldPath: String, `type`: String, allowedTypes: Seq[String]): Option[ValidationMessage] = {
      if(allowedTypes.contains(`type`.toLowerCase)) {
        None
      } else {
        Some(ValidationMessage(fieldPath, s"Author is of illegal type. Must be one of ${allowedTypes.mkString(", ")}"))
      }
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
