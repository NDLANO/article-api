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
  val IllegalContentInBasicText = s"The content contains illegal tags. Allowed HTML tags are: ${HTMLCleaner.legalTags.mkString(",")}"
  val IllegalContentInPlainText = "The content contains illegal html-characters. No HTML is allowed"
  val FieldEmpty = "Required field is empty"

  val validate: (String, String) => Option[ValidationMessage] =
    allowHtml match {
      case true => validateOnlyBasicHtmlTags
      case false => validateNoHtmlTags
    }

  private def validateOnlyBasicHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
    text.isEmpty match {
      case true => Some(ValidationMessage(fieldPath, FieldEmpty))
      case false => {
        Jsoup.isValid(text, new Whitelist().addTags(HTMLCleaner.legalTags.toSeq: _*)) match {
          case true => None
          case false => Some(ValidationMessage(fieldPath, IllegalContentInBasicText))
        }
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
