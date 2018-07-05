package no.ndla.articleapi.model.search

import no.ndla.articleapi._
import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.Formats
import org.json4s.native.Serialization.{writePretty, read}

class SearchableArticleSerializerTest extends UnitSuite with TestEnvironment {
  implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

  val searchableArticle1 = SearchableArticle(
    id = 10.toLong,
    title = SearchableLanguageValues(Vector(LanguageValue("nb", "tittel"), LanguageValue("en", "title"))),
    content = SearchableLanguageValues(Vector(LanguageValue("nb", "innhold"), LanguageValue("en", "content"))),
    visualElement = SearchableLanguageValues(Vector(LanguageValue("nb", "visueltelement"), LanguageValue("en", "visualelement"))),
    introduction = SearchableLanguageValues(Vector(LanguageValue("nb", "ingress"), LanguageValue("en", "introduction"))),
    metaDescription = SearchableLanguageValues(Vector(LanguageValue("nb", "meta beskrivelse"), LanguageValue("en", "meta description"))),
    metaImage = SearchableLanguageValues(Vector(LanguageValue("nb", "1"))),
    tags = SearchableLanguageList(Vector(LanguageValue("nb", List("m", "e", "r", "k")), LanguageValue("en", List("t", "a", "g", "s")))),
    lastUpdated = new DateTime(2018, 2, 22, 14, 0, 51, DateTimeZone.UTC).withMillisOfSecond(0),
    license = "by-sa",
    authors = Seq("Jonas Natty"),
    articleType = "standard",
    defaultTitle = Some("tjuppidu")
  )

  val searchableConcept1 = SearchableConcept(
    id = 5.toLong,
    title = SearchableLanguageValues(Seq(LanguageValue("nb", "tittel"), LanguageValue("en", "title"))),
    content = SearchableLanguageValues(Seq(LanguageValue("nb", "innhold"), LanguageValue("en", "content"))),
    defaultTitle = Some("tjappsipappsi")
  )

  test("That deserialization and serialization of SearchableArticle works as expected") {
    val json = writePretty(searchableArticle1)
    val deserialized = read[SearchableArticle](json)

    deserialized should be(searchableArticle1)
  }

  test("That deserialization and serialization of SearchableConcept works as expected") {
    val json = writePretty(searchableConcept1)
    val deserialized = read[SearchableConcept](json)

    deserialized should be(searchableConcept1)
  }

}
