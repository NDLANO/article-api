/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.search

import java.util.Date

import no.ndla.articleapi.model.domain.emptySomeToNone
import no.ndla.articleapi.model.search.LanguageValue.{LanguageValue => LV}
import org.json4s.JsonAST.{JArray, JField, JObject, JString}


object LanguageValue {
  case class LanguageValue[T](lang: String, value: T)
  def apply[T](lang: String, value: T): LanguageValue[T] = LanguageValue(lang, value)
}

case class SearchableLanguageValues(languageValues: Seq[LV[String]]) {
  def toJsonField(name: String): Seq[JField] =
    languageValues.map(lv => JField(s"$name.${lv.lang}", JString(lv.value)))
}

case class SearchableLanguageList(languageValues: Seq[LV[Seq[String]]]) {
  def toJsonField(name: String): Seq[JField] =
    languageValues.map(lv => JField(s"$name.${lv.lang}", JArray(lv.value.map(JString).toList)))
}

object SearchableLanguageValues {
  /**
    * Apply method to create SearchableLanguageValues object from jsonObject for fields with name.
    * Fields are should be named "name.language" in the jsonObject
    * @param name Name of the field without language.
    * @param jsonObject Parent object containing all fields.
    * @return SearchableLanguageValues object containing every Language with name name.
    */
  def apply(name: String, jsonObject: JObject): SearchableLanguageValues = {
    val languageValues = jsonObject.obj.flatMap{
      case (key, value: JString) =>
        val split = key.split('.')
        split match {
          case Array(keyName: String, language: String) if keyName == name =>
            Some(LanguageValue[String](language, value.s))
          case _ => None
        }
      case _ => None
    }
    SearchableLanguageValues(languageValues)
  }
}

object SearchableLanguageList {
  /**
    * Apply method to create SearchableLanguageList object from jsonObject for fields with name.
    * Fields are should be named "name.language" in the jsonObject
    * @param name Name of the field without language.
    * @param jsonObject Parent object containing all fields.
    * @return SearchableLanguageList object containing every Language with name name.
    */
  def apply(name: String, jsonObject: JObject): SearchableLanguageList = {
    val languageValues = jsonObject.obj.flatMap{
      case (key, value: JArray) =>
        val split = key.split('.')
        split match {
          case Array(keyName: String, language: String) if keyName == name =>

            val valueArray = value.arr.flatMap{
              case e: JString => Some(e.s)
              case _ => None
            }

            Some(LanguageValue[Seq[String]](language, valueArray))
          case _ => None
        }
      case _ => None
    }

    SearchableLanguageList(languageValues)
  }
}

case class SearchableArticle(
  id: Long,
  title: SearchableLanguageValues,
  content: SearchableLanguageValues,
  visualElement: SearchableLanguageValues,
  introduction: SearchableLanguageValues,
  metaDescription: SearchableLanguageValues,
  metaImage: SearchableLanguageValues,
  tags: SearchableLanguageList,
  lastUpdated: Date,
  license: String,
  authors: Seq[String],
  articleType: String,
  defaultTitle: Option[String]
)

case class SearchableConcept(
  id: Long,
  title: SearchableLanguageValues,
  content: SearchableLanguageValues,
  defaultTitle: Option[String]
)
