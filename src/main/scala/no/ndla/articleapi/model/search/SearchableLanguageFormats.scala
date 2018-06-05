package no.ndla.articleapi.model.search

import java.util.Date
import org.json4s.JsonAST.{JArray, JField, JObject, JString}
import org.json4s.{CustomSerializer, Extraction, MappingException}
import org.json4s._


object LanguagelessSearchableArticle {

  case class LanguagelessSearchableArticle(
                                            id: Long,
                                            lastUpdated: Date,
                                            license: String,
                                            authors: Seq[String],
                                            articleType: String,
                                            defaultTitle: Option[String]
                                          )

  def apply(searchableArticle: SearchableArticle): LanguagelessSearchableArticle = {
    LanguagelessSearchableArticle(
      searchableArticle.id,
      searchableArticle.lastUpdated,
      searchableArticle.license,
      searchableArticle.authors,
      searchableArticle.articleType,
      searchableArticle.defaultTitle
    )
  }
}


object LanguagelessSearchableConcept {

  case class LanguagelessSearchableConcept(
                                            id: Long,
                                            defaultTitle: Option[String]
                                          )

  def apply(searchableConcept: SearchableConcept): LanguagelessSearchableConcept = {
    LanguagelessSearchableConcept(
      searchableConcept.id,
      searchableConcept.defaultTitle
    )
  }
}

class SearchableArticleSerializer extends CustomSerializer[SearchableArticle](_ => ( {
  case obj: JObject =>
    implicit val formats = org.json4s.DefaultFormats
    SearchableArticle(
      id = (obj \ "id").extract[Long],
      title = SearchableLanguageValues("title", obj),
      content = SearchableLanguageValues("content", obj),
      visualElement = SearchableLanguageValues("visualElement", obj),
      introduction = SearchableLanguageValues("introduction", obj),
      metaDescription = SearchableLanguageValues("metaDescription", obj),
      metaImage = SearchableLanguageValues("metaImage", obj),
      tags = SearchableLanguageList("tags", obj),
      lastUpdated = (obj \ "lastUpdated").extract[Date],
      license = (obj \ "license").extract[String],
      authors = (obj \ "authors").extract[Seq[String]],
      articleType = (obj \ "articleType").extract[String],
      defaultTitle = (obj \ "defaultTitle").extract[Option[String]]
    )
}, {
  case article: SearchableArticle =>
    implicit val formats = org.json4s.DefaultFormats
    val languageFields =
      List(
        article.title.toJsonField("title"),
        article.content.toJsonField("content"),
        article.visualElement.toJsonField("visualElement"),
        article.introduction.toJsonField("introduction"),
        article.metaDescription.toJsonField("metaDescription"),
        article.metaImage.toJsonField("metaImage"),
        article.tags.toJsonField("tags")
      ).flatMap {
        case l: Seq[JField] => l
        case _ => Seq.empty
      }
    val partialSearchableArticle = LanguagelessSearchableArticle(article)
    val partialJObject = Extraction.decompose(partialSearchableArticle)
    partialJObject.merge(JObject(languageFields: _*))
}))

class SearchableConceptSerializer extends CustomSerializer[SearchableConcept](_ => ( {
  case obj: JObject =>
    implicit val formats = org.json4s.DefaultFormats
    SearchableConcept(
      id = (obj \ "id").extract[Long],
      title = SearchableLanguageValues("title", obj),
      content = SearchableLanguageValues("content", obj),
      defaultTitle = (obj \ "defaultTitle").extract[Option[String]]
    )
}, {
  case concept: SearchableConcept =>
    implicit val formats = org.json4s.DefaultFormats
    val languageFields =
      List(
        concept.title.toJsonField("title"),
        concept.content.toJsonField("content")
      ).flatMap {
        case l: Seq[JField] => l
        case _ => Seq.empty
      }

    val partialSearchableConcept = LanguagelessSearchableConcept(concept)
    val partialJObject = Extraction.decompose(partialSearchableConcept)
    partialJObject.merge(JObject(languageFields: _*))
}))

object SearchableLanguageFormats {
  val JSonFormats: Formats =
    org.json4s.DefaultFormats +
      new SearchableArticleSerializer +
      new SearchableConceptSerializer
}
