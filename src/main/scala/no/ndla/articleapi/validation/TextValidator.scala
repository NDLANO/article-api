/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.validation

import no.ndla.articleapi.model.api.ValidationMessage
import no.ndla.articleapi.service.converters.HTMLCleaner
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist

class TextValidator(allowHtml: Boolean) {
  private def IllegalContentInBasicText = s"The content contains illegal tags and/or attributes. Allowed HTML tags are: ${HTMLCleaner.allLegalTags.mkString(",")}"
  private val IllegalContentInPlainText = "The content contains illegal html-characters. No HTML is allowed"
  private val FieldEmpty = "Required field is empty"
  private val EmbedTagValidator = new EmbedTagValidator

  def validate(fieldPath: String, text: String): Seq[ValidationMessage] = {
    allowHtml match {
      case true => validateOnlyBasicHtmlTags(fieldPath, text)
      case false => validateNoHtmlTags(fieldPath, text).toList
    }
  }

  private def validateOnlyBasicHtmlTags(fieldPath: String, text: String): Seq[ValidationMessage] = {
    val whiteList = new Whitelist().addTags(HTMLCleaner.allLegalTags.toSeq: _*)
    HTMLCleaner.allLegalTags
      .filter(tag => HTMLCleaner.legalAttributesForTag(tag).nonEmpty)
      .foreach(tag => whiteList.addAttributes(tag, HTMLCleaner.legalAttributesForTag(tag).toSeq: _*))

    text.isEmpty match {
      case true => ValidationMessage(fieldPath, FieldEmpty) :: Nil
      case false => {
        val jsoupValidatorMessages = Jsoup.isValid(text, whiteList) match {
          case true => None
          case false => Some(ValidationMessage(fieldPath, IllegalContentInBasicText))
        }
        val embedTagValidatorMessages = EmbedTagValidator.validate(fieldPath, text)
        jsoupValidatorMessages.toList ++ embedTagValidatorMessages
      }

    }
  }

  private def validateNoHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
    Jsoup.isValid(text, Whitelist.none()) match {
      case true => None
      case false => Some(ValidationMessage(fieldPath, IllegalContentInPlainText))
    }
  }
}
