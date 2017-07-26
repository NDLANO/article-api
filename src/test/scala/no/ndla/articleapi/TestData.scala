/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import no.ndla.articleapi.integration._
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.model.api
import org.joda.time.{DateTime, DateTimeZone}

object TestData {
  private val publicDomainCopyright= Copyright("publicdomain", "", List())
  private val byNcSaCopyright = Copyright("by-nc-sa", "Gotham City", List(Author("Forfatter", "DC Comics")))
  private val copyrighted = Copyright("copyrighted", "New York", List(Author("Forfatter", "Clark Kent")))
  private val today = new DateTime().toDate

  private val embedUrl = "http://www.example.org"

  val (articleId, externalId) = (1, "751234")

  val sampleArticleV2 = api.ArticleV2(
    id=1,
    oldNdlaUrl = None,
    revision=1,
    language="nb",
    title="title",
    content="this is content",
    footNotes= Some(Map(("something", api.FootNoteItem("title", "type", "year", "edition", "publisher", Seq("author"))))),
    copyright = api.Copyright(api.License("licence", None, None), "origin", Seq(api.Author("developer", "Per"))),
    tags = Seq("tag"),
    requiredLibraries = Seq(api.RequiredLibrary("JS", "JavaScript", "url")),
    visualElement = None,
    introduction = None,
    metaDescription = "metaDesc",
    created = new DateTime(2017, 1, 1, 12, 15, 32, DateTimeZone.UTC).toDate,
    updated = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate,
    updatedBy = "me",
    articleType = "standard",
    supportedLanguages = Seq("nb")
  )

  val articleHit1 = """
                      |{
                      |  "id": "4",
                      |  "title": [
                      |    {
                      |      "title": "8. mars, den internasjonale kvinnedagen",
                      |      "language": "nb"
                      |    },
                      |    {
                      |      "title": "8. mars, den internasjonale kvinnedagen",
                      |      "language": "nn"
                      |    }
                      |  ],
                      |  "introduction": [
                      |    {
                      |      "introduction": "8. mars er den internasjonale kvinnedagen.",
                      |      "language": "nb"
                      |    },
                      |    {
                      |      "introduction": "8. mars er den internasjonale kvinnedagen.",
                      |      "language": "nn"
                      |    }
                      |  ],
                      |  "url": "http://localhost:30002/article-api/v1/articles/4",
                      |  "license": "by-sa",
                      |  "articleType": "standard"
                      |}
                    """.stripMargin

  val sampleArticle = api.Article(
    articleId.toString,
    Some(s"//red.ndla.no/node/$externalId"),
    2,
    Seq(api.ArticleTitle("title", Option("nb"))),
    Seq(api.ArticleContent("content", None, Option("nb"))),
    api.Copyright(api.License("by", Some("Creative Commons Attribution 2.0 Generic"), Some("https://creativecommons.org/licenses/by/2.0/")), "", Seq()),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    Seq(api.ArticleMetaDescription("meta description", Option("nb"))),
    today,
    today,
    "ndalId54321",
    "standard"
  )

  val apiArticleV2 = api.ArticleV2(
    articleId,
    Some(s"//red.ndla.no/node/$externalId"),
    2,
    "nb",
    "title",
    "content",
    None,
    api.Copyright(api.License("by", Some("Creative Commons Attribution 2.0 Generic"), Some("https://creativecommons.org/licenses/by/2.0/")), "", Seq()),
    Seq(),
    Seq(),
    None,
    None,
    "meta description",
    today,
    today,
    "ndalId54321",
    "standard",
    Seq("nb")
  )


  val requestNewArticleV2Body = """
                                  |{
                                  |  "copyright": {
                                  |    "license": {
                                  |      "license": "by-sa",
                                  |      "description": "something"
                                  |    },
                                  |    "origin": "fromSomeWhere",
                                  |    "authors": [
                                  |      {
                                  |        "type": "string",
                                  |        "name": "Christian P"
                                  |      }
                                  |    ]
                                  |  },
                                  |  "language": "nb",
                                  |  "visualElement": "string",
                                  |  "introduction": "string",
                                  |  "metaDescription": "string",
                                  |  "tags": [
                                  |	    "string"
                                  |	  ],
                                  |  "content": "string",
                                  |  "footNotes": [ "string " ],
                                  |  "title": "string",
                                  |  "articleType": "standard",
                                  |  "metaImageId": "22",
                                  |  "requiredLibraries": [
                                  |    {
                                  |      "mediaType": "string",
                                  |      "name": "string"
                                  |    }
                                  |  ]
                                  |}
                                """.stripMargin

  val sampleArticleWithPublicDomain = Article(
    Option(1),
    Option(1),
    Seq(ArticleTitle("test", Option("en"))),
    Seq(ArticleContent("<article><div>test</div></article>", None, Option("en"))),
    publicDomainCopyright,
    Seq(),
    Seq(),
    Seq(VisualElement("image", Some("en"))),
    Seq(ArticleIntroduction("This is an introduction", Some("en"))),
    Seq(),
    None,
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate,
    "ndalId54321",
    ArticleType.Standard.toString)

  val sampleDomainArticle = Article(
    Option(articleId),
    Option(2),
    Seq(ArticleTitle("title", Option("nb"))),
    Seq(ArticleContent("content", None, Option("nb"))),
    Copyright("by", "", Seq()),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    Seq(ArticleMetaDescription("meta description", Option("nb"))),
    None,
    today,
    today,
    "ndalId54321",
    ArticleType.Standard.toString
  )

  val sampleDomainArticle2 = Article(
    None,
    None,
    Seq(ArticleTitle("test", Option("en"))),
    Seq(ArticleContent("<article><div>test</div></article>", None, Option("en"))),
    Copyright("publicdomain", "", Seq()),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    None,
    today,
    today,
    "ndalId54321",
    ArticleType.Standard.toString
  )

  val newArticle = api.NewArticle(
    Seq(api.ArticleTitle("test", Option("en"))),
    Seq(api.ArticleContent("<article><div>test</div></article>", None, Option("en"))),
    Seq(),
    None,
    None,
    None,
    None,
    api.Copyright(api.License("publicdomain", None, None), "", Seq()),
    None,
    "standard"
  )

  val newArticleV2 = api.NewArticleV2(
    "test",
    "<article><div>test</div></article>",
    None,
    Seq(),
    None,
    None,
    None,
    None,
    api.Copyright(api.License("publicdomain", None, None), "", Seq()),
    None,
    "standard",
    "en"
  )

  val sampleArticleWithByNcSa = sampleArticleWithPublicDomain.copy(copyright=byNcSaCopyright)
  val sampleArticleWithCopyrighted = sampleArticleWithPublicDomain.copy(copyright=copyrighted )

  val updatedArticle = api.UpdatedArticle(
    Seq(api.ArticleTitle("test", Option("en"))),
    1,
    Seq(api.ArticleContent("<article><div>test</div></article>", None, Option("en"))),
    Seq.empty,
    Seq.empty,
    Seq.empty,
    None,
    Seq.empty,
    Some(api.Copyright(api.License("publicdomain", None, None), "", Seq.empty)),
    Seq.empty,
    Some("standard")
  )

  val apiArticleWithHtmlFault = api.Article(
    "1",
    None,
    1,
    Seq(api.ArticleTitle("test", Option("en"))),
    Seq(api.ArticleContent(
      """<ul><li><h1>Det er ikke lov å gjøre dette.</h1> Tekst utenfor.</li><li>Dette er helt ok</li></ul>
        |<ul><li><h2>Det er ikke lov å gjøre dette.</h2></li><li>Dette er helt ok</li></ul>
        |<ol><li><h3>Det er ikke lov å gjøre dette.</h3></li><li>Dette er helt ok</li></ol>
        |<ol><li><h4>Det er ikke lov å gjøre dette.</h4></li><li>Dette er helt ok</li></ol>
      """.stripMargin, None, Option("en"))),
    api.Copyright(api.License("publicdomain", None, None), "", Seq()),
    Nil,
    Nil,
    Nil,
    Nil,
    Nil,
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate,
    "ndalId54321",
    "standard"
  )

  val (nodeId, nodeId2) = ("1234", "4321")
  val sampleTitle = ArticleTitle("title", Some("en"))
  val sampleContent = LanguageContent(nodeId, nodeId, "sample content", "metadescription",  Some("en"), None)
  val sampleTranslationContent = sampleContent.copy(tnid=nodeId2)

  val visualElement = VisualElement(s"""<$resourceHtmlEmbedTag  data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" />""", Some("nb"))

  val sampleImageMetaInformation = ImageMetaInformation(
    "1",
    List(ImageTitle("Sample title", Some("nb"))),
    List(ImageAltText("alt text", Some("nb"))),
    "://image-url.com/image/img.jpg",
    1024,
    "application/jpeg",
    ImageCopyright(ImageLicense("by", "Creative Commons", None), "pix", Seq.empty),
    List(ImageTag(Seq("sample tag"), Some("nb"))))

  val sampleConcept = Concept(
    Some(1),
    Seq(ConceptTitle("Tittel for begrep", Some("nb"))),
    Seq(ConceptContent("Innhold for begrep", Some("nb"))),
    Seq(),
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate
  )

  val sampleApiConcept = api.Concept(
    1,
    "Tittel for begrep",
    "Innhold for begrep",
    Seq(),
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate,
    "nb",
    Seq("nb")
  )

}


