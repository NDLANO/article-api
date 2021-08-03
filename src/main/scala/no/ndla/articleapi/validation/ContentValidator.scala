/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.validation

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.ArticleApiProperties.{BrightcoveVideoScriptUrl, NRKVideoScriptUrl, MinimumAllowedTags}
import no.ndla.articleapi.integration.DraftApiClient
import no.ndla.articleapi.model.domain._
import no.ndla.mapping.ISO639.get6391CodeFor6392CodeMappings
import no.ndla.mapping.License.getLicense
import no.ndla.validation.{TextValidator, ValidationException, ValidationMessage}
import no.ndla.validation.HtmlTagRules.stringToJsoupDocument

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

trait ContentValidator {
  this: DraftApiClient =>
  val contentValidator: ContentValidator

  class ContentValidator(allowEmptyLanguageField: Boolean) {
    private val NoHtmlValidator = new TextValidator(allowHtml = false)
    private val HtmlValidator = new TextValidator(allowHtml = true)

    def softValidateArticle(article: Article, isImported: Boolean): Try[Article] = {
      val metaValidation =
        if (isImported) None else validateNonEmpty("metaDescription", article.metaDescription)
      val validationErrors =
        validateArticleType(article.articleType) ++
          validateNonEmpty("content", article.content) ++
          validateNonEmpty("title", article.title) ++
          metaValidation

      if (validationErrors.isEmpty) {
        Success(article)
      } else {
        Failure(new ValidationException(errors = validationErrors))
      }
    }

    def validateArticle(article: Article, allowUnknownLanguage: Boolean, isImported: Boolean = false): Try[Article] = {
      val validationErrors = validateArticleContent(article.content, allowUnknownLanguage) ++
        article.introduction.flatMap(i => validateIntroduction(i, allowUnknownLanguage)) ++
        validateMetaDescription(article.metaDescription, allowUnknownLanguage, isImported) ++
        validateTitle(article.title, allowUnknownLanguage) ++
        validateCopyright(article.copyright) ++
        validateTags(article.tags, allowUnknownLanguage, isImported) ++
        article.requiredLibraries.flatMap(validateRequiredLibrary) ++
        article.metaImage.flatMap(validateMetaImage) ++
        article.visualElement.flatMap(v => validateVisualElement(v, allowUnknownLanguage)) ++
        validateArticleType(article.articleType)

      if (validationErrors.isEmpty) {
        Success(article)
      } else {
        Failure(new ValidationException(errors = validationErrors))
      }
    }

    private def validateNonEmpty(field: String, values: Seq[LanguageField[_]]): Option[ValidationMessage] = {
      if (values.isEmpty || values.forall(_.isEmpty)) {
        Some(ValidationMessage(field, "Field must contain at least one entry"))
      } else
        None
    }

    private def validateArticleType(articleType: String): Seq[ValidationMessage] = {
      ArticleType.valueOf(articleType) match {
        case None =>
          Seq(
            ValidationMessage(
              "articleType",
              s"$articleType is not a valid article type. Valid options are ${ArticleType.all.mkString(",")}"))
        case _ => Seq.empty
      }
    }

    private def validateArticleContent(contents: Seq[ArticleContent],
                                       allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      contents.flatMap(content => {
        val field = s"content.${content.language}"
        HtmlValidator.validate(field, content.content).toList ++
          rootElementContainsOnlySectionBlocks(field, content.content) ++
          validateLanguage("content.language", content.language, allowUnknownLanguage)
      }) ++ validateNonEmpty("content", contents)
    }

    def rootElementContainsOnlySectionBlocks(field: String, html: String): Option[ValidationMessage] = {
      val legalTopLevelTag = "section"
      val topLevelTags = stringToJsoupDocument(html).children().asScala.map(_.tagName())

      topLevelTags.forall(_ == legalTopLevelTag) match {
        case true => None
        case false =>
          val illegalTags = topLevelTags.filterNot(_ == legalTopLevelTag).mkString(",")
          Some(
            ValidationMessage(
              field,
              s"An article must consist of one or more <section> blocks. Illegal tag(s) are $illegalTags "))
      }
    }

    private def validateVisualElement(content: VisualElement, allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      val field = s"visualElement.${content.language}"
      HtmlValidator
        .validate(field, content.resource, requiredToOptional = Map("image" -> Seq("data-caption")))
        .toList ++
        validateLanguage("visualElement.language", content.language, allowUnknownLanguage)
    }

    private def validateIntroduction(content: ArticleIntroduction,
                                     allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      val field = s"introduction.${content.language}"
      NoHtmlValidator.validate(field, content.introduction).toList ++
        validateLanguage("introduction.language", content.language, allowUnknownLanguage)
    }

    private def validateMetaDescription(contents: Seq[ArticleMetaDescription],
                                        allowUnknownLanguage: Boolean,
                                        allowEmpty: Boolean): Seq[ValidationMessage] = {
      val nonEmptyValidation = if (allowEmpty) None else validateNonEmpty("metaDescription", contents)
      val validations = contents.flatMap(content => {
        val field = s"metaDescription.${content.language}"
        NoHtmlValidator.validate(field, content.content).toList ++
          validateLanguage("metaDescription.language", content.language, allowUnknownLanguage)
      })
      validations ++ nonEmptyValidation
    }

    private def validateTitle(titles: Seq[LanguageField[String]],
                              allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      titles.flatMap(title => {
        val field = s"title.$language"
        NoHtmlValidator.validate(field, title.value).toList ++
          validateLanguage("title.language", title.language, allowUnknownLanguage) ++
          validateLength("title", title.value, 256)
      }) ++ validateNonEmpty("title", titles)
    }

    private def validateCopyright(copyright: Copyright): Seq[ValidationMessage] = {
      val licenseMessage = validateLicense(copyright.license)
      val contributorsMessages =
        copyright.creators.flatMap(a => validateAuthor(a, "copyright.creators", ArticleApiProperties.creatorTypes)) ++
          copyright.processors.flatMap(a =>
            validateAuthor(a, "copyright.processors", ArticleApiProperties.processorTypes)) ++
          copyright.rightsholders.flatMap(a =>
            validateAuthor(a, "copyright.rightsholders", ArticleApiProperties.rightsholderTypes))
      val originMessage = NoHtmlValidator.validate("copyright.origin", copyright.origin)
      val agreementMessage = validateAgreement(copyright)

      licenseMessage ++ contributorsMessages ++ originMessage ++ agreementMessage
    }

    def validateAgreement(copyright: Copyright): Seq[ValidationMessage] = {
      copyright.agreementId match {
        case Some(id) =>
          draftApiClient.agreementExists(id) match {
            case false => Seq(ValidationMessage("copyright.agreement", s"Agreement with id $id does not exist"))
            case _     => Seq()
          }
        case _ => Seq()
      }
    }

    private def validateLicense(license: String): Seq[ValidationMessage] = {
      getLicense(license) match {
        case None => Seq(ValidationMessage("license.license", s"$license is not a valid license"))
        case _    => Seq()
      }
    }

    private def validateAuthor(author: Author, fieldPath: String, allowedTypes: Seq[String]): Seq[ValidationMessage] = {
      NoHtmlValidator.validate(s"$fieldPath.type", author.`type`).toList ++
        NoHtmlValidator.validate(s"$fieldPath.name", author.name).toList ++
        validateAuthorType(s"$fieldPath.type", author.`type`, allowedTypes).toList
    }

    def validateAuthorType(fieldPath: String, `type`: String, allowedTypes: Seq[String]): Option[ValidationMessage] = {
      if (allowedTypes.contains(`type`.toLowerCase)) {
        None
      } else {
        Some(ValidationMessage(fieldPath, s"Author is of illegal type. Must be one of ${allowedTypes.mkString(", ")}"))
      }
    }

    private def validateTags(tags: Seq[ArticleTag],
                             allowUnknownLanguage: Boolean,
                             isImported: Boolean): Seq[ValidationMessage] = {

      // Since quite a few articles from old ndla has fewer than 3 tags we skip validation here for imported articles until we are done importing.
      val languageTagAmountErrors = tags.groupBy(_.language).flatMap {
        case (lang, tagsForLang) if !isImported && tagsForLang.flatMap(_.tags).size < MinimumAllowedTags =>
          Seq(
            ValidationMessage(s"tags.$lang",
                              s"Invalid amount of tags. Articles needs $MinimumAllowedTags or more tags to be valid."))
        case _ => Seq()
      }

      val noTagsError =
        if (tags.isEmpty) Seq(ValidationMessage("tags", "The article must have at least one set of tags")) else Seq()

      tags.flatMap(tagList => {
        tagList.tags.flatMap(NoHtmlValidator.validate(s"tags.${tagList.language}", _)).toList :::
          validateLanguage("tags.language", tagList.language, allowUnknownLanguage).toList
      }) ++ languageTagAmountErrors ++ noTagsError
    }

    private def validateRequiredLibrary(requiredLibrary: RequiredLibrary): Option[ValidationMessage] = {
      val permittedLibraries = Seq(BrightcoveVideoScriptUrl) ++ NRKVideoScriptUrl
      permittedLibraries.contains(requiredLibrary.url) match {
        case false =>
          Some(ValidationMessage(
            "requiredLibraries.url",
            s"${requiredLibrary.url} is not a permitted script. Allowed scripts are: ${permittedLibraries.mkString(",")}"))
        case true => None
      }
    }

    private def validateMetaImage(metaImage: ArticleMetaImage): Seq[ValidationMessage] =
      (validateMetaImageId(metaImage.imageId) ++ validateMetaImageAltText(metaImage.altText)).toSeq

    private def validateMetaImageAltText(altText: String): Seq[ValidationMessage] =
      NoHtmlValidator.validate("metaImage.alt", altText)

    private def validateMetaImageId(id: String) = {
      def isAllDigits(x: String) = x forall Character.isDigit
      isAllDigits(id) match {
        case true if id.size > 0 => None
        case _                   => Some(ValidationMessage("metaImageId", "Meta image ID must be a number"))
      }
    }

    private def validateLanguage(fieldPath: String,
                                 languageCode: String,
                                 allowUnknownLanguage: Boolean): Option[ValidationMessage] = {
      languageCode.nonEmpty && languageCodeSupported6391(languageCode, allowUnknownLanguage) match {
        case true  => None
        case false => Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
      }
    }

    private def validateLength(fieldPath: String, content: String, maxLength: Int): Option[ValidationMessage] = {
      if (content.length > maxLength)
        Some(ValidationMessage(fieldPath, s"This field exceeds the maximum permitted length of $maxLength characters"))
      else
        None
    }

    private def languageCodeSupported6391(languageCode: String, allowUnknownLanguage: Boolean): Boolean = {
      val languageCodes = get6391CodeFor6392CodeMappings.values.toSeq ++ (if (allowUnknownLanguage) Seq("unknown")
                                                                          else Seq.empty)
      languageCodes.contains(languageCode)
    }
  }
}
