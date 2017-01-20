/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.validation

import no.ndla.articleapi.ArticleApiProperties.{H5PResizerScriptUrl, NDLABrightcoveVideoScriptUrl, NRKVideoScriptUrl, resourceHtmlEmbedTag}
import no.ndla.articleapi.model.api.{ValidationException, ValidationMessage}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.service.converters.HTMLCleaner
import no.ndla.mapping.ISO639.get6391CodeFor6392CodeMappings
import no.ndla.mapping.License.getLicense
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist

trait ArticleValidator {
  val validationService: ArticleValidator

  class ArticleValidator {
    def validateArticle(article: Article) = {
      val validationErrors = article.content.flatMap(validateContent) ++
        article.introduction.flatMap(validateIntroduction) ++
        article.metaDescription.flatMap(validateMetaDescription) ++
        article.title.flatMap(validateTitle) ++
        validateCopyright(article.copyright) ++
        validateTags(article.tags) ++
        article.requiredLibraries.flatMap(validateRequiredLibrary) ++
        article.metaImageId.flatMap(validateMetaImageId) ++
        validateContentType(article.contentType)

      // TODO: how to validate visualElement: Answer: embed tag validator

      if (validationErrors.nonEmpty)
        throw new ValidationException(errors=validationErrors)
    }

    def validateContent(content: ArticleContent): Seq[ValidationMessage] = {
      validateOnlyPermittedHtmlTags("content.content", content.content).toList ++
        validateLanguage("content.language", content.language)
    }

    def validateIntroduction(content: ArticleIntroduction): Seq[ValidationMessage] = {
      validateNoHtmlTags("introduction.introduction", content.introduction).toList ++
        validateLanguage("introduction.language", content.language)
    }

    def validateMetaDescription(content: ArticleMetaDescription): Seq[ValidationMessage] = {
      validateNoHtmlTags("metaDescription.metaDescription", content.content).toList ++
        validateLanguage("metaDescription.language", content.language)
    }

    def validateTitle(content: ArticleTitle): Seq[ValidationMessage] = {
      validateNoHtmlTags("title.title", content.title).toList ++
        validateLanguage("title.language", content.language)
    }

    def validateCopyright(copyright: Copyright): Seq[ValidationMessage] = {
      val licenseMessage = validateLicense(copyright.license)
      val contributorsMessages = copyright.authors.flatMap(validateAuthor)
      val originMessage = validateNoHtmlTags("copyright.origin", copyright.origin)

      licenseMessage ++ contributorsMessages ++ originMessage
    }

    def validateLicense(license: String): Seq[ValidationMessage] = {
      getLicense(license) match {
        case None => Seq(new ValidationMessage("license.license", s"$license is not a valid license"))
        case _ => Seq()
      }
    }

    def validateAuthor(author: Author): Seq[ValidationMessage] = {
      validateNoHtmlTags("author.type", author.`type`).toList ++
        validateNoHtmlTags("author.name", author.name).toList
    }

    def validateTags(tags: Seq[ArticleTag]): Seq[ValidationMessage] = {
      tags.flatMap(tagList => {
        tagList.tags.flatMap(validateNoHtmlTags("tags.tags", _)).toList :::
          validateLanguage("tags.language", tagList.language).toList
      })
    }

    def validateRequiredLibrary(requiredLibrary: RequiredLibrary): Option[ValidationMessage] = {
      val permittedLibraries = Seq(NDLABrightcoveVideoScriptUrl, H5PResizerScriptUrl, NRKVideoScriptUrl) // TODO find better way of generating this list. (avoid hardcoding list)
      permittedLibraries.contains(requiredLibrary.url) match {
        case false => Some(ValidationMessage("requiredLibraries.url", s"${requiredLibrary.url} is not a permitted script. Allowed scripts are: ${permittedLibraries.mkString(",")}"))
        case true => None
      }
    }

    def validateMetaImageId(metaImageId: String): Option[ValidationMessage] = {
      validateNoHtmlTags("metaImageId", metaImageId)
    }

    def validateContentType(contentType: String): Option[ValidationMessage] = {
      validateNoHtmlTags("contentType", contentType)
    }


    private def validateOnlyPermittedHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
      text.isEmpty match {
        case true => Some(ValidationMessage(fieldPath, "Required field is empty"))
        case false => {
          Jsoup.isValid(text, new Whitelist().addTags(HTMLCleaner.legalTags.toList: _*)) match {
            case true => None
            case false => Some(ValidationMessage(fieldPath, s"The content contains illegal tags. Allowed html tags are: ${HTMLCleaner.legalTags.mkString(",")}"))
          }
        }
      }
    }

    private def validateNoHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
      Jsoup.isValid(text, Whitelist.none()) match {
        case true => None
        case false => Some(ValidationMessage(fieldPath, "No html is allowed in this field"))
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
