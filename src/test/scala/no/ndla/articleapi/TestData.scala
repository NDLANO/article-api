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
import no.ndla.articleapi.ArticleApiProperties._
import no.ndla.validation.EmbedTagRules.ResourceHtmlEmbedTag
import no.ndla.articleapi.model.api
import org.joda.time.{DateTime, DateTimeZone}

object TestData {
  private val publicDomainCopyright= Copyright("publicdomain", "", List(), List(), List(), None, None, None)
  private val byNcSaCopyright = Copyright("by-nc-sa", "Gotham City", List(Author("Writer", "DC Comics")), List(), List(), None, None, None)
  private val copyrighted = Copyright("copyrighted", "New York", List(Author("Writer", "Clark Kent")), List(), List(), None, None, None)
  private val today = new DateTime().toDate

  private val embedUrl = "http://www.example.org"

  val (articleId, externalId) = (1, "751234")

  val sampleArticleV2 = api.ArticleV2(
    id=1,
    oldNdlaUrl = None,
    revision=1,
    title=api.ArticleTitle("title", "nb"),
    content=api.ArticleContentV2("this is content", "nb"),
    copyright = api.Copyright(api.License("licence", None, None), "origin", Seq(api.Author("developer", "Per")), List(), List(), None, None, None),
    tags = api.ArticleTag(Seq("tag"), "nb"),
    requiredLibraries = Seq(api.RequiredLibrary("JS", "JavaScript", "url")),
    visualElement = None,
    metaImage = None,
    introduction = None,
    metaDescription = api.ArticleMetaDescription("metaDesc", "nb"),
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
                      |  "url": "http://localhost:30002/article-api/v2/articles/4",
                      |  "license": "by-sa",
                      |  "articleType": "standard"
                      |}
                    """.stripMargin

  val apiArticleV2 = api.ArticleV2(
    articleId,
    Some(s"//red.ndla.no/node/$externalId"),
    2,
    api.ArticleTitle("title", "nb"),
    api.ArticleContentV2("content", "nb"),
    api.Copyright(api.License("by", Some("Creative Commons Attribution 2.0 Generic"), Some("https://creativecommons.org/licenses/by/2.0/")), "", List(), List(), List(), None, None, None),
    api.ArticleTag(Seq(), "nb"),
    Seq(),
    None,
    Some(s"${externalApiUrls("raw-image")}/11"),
    None,
    api.ArticleMetaDescription("meta description", "nb"),
    today,
    today,
    "ndalId54321",
    "standard",
    Seq("nb")
  )


  val sampleArticleWithPublicDomain = Article(
    Option(1),
    Option(1),
    Seq(ArticleTitle("test", "en")),
    Seq(ArticleContent("<section><div>test</div></section>", "en")),
    publicDomainCopyright,
    Seq(),
    Seq(),
    Seq(VisualElement("image", "en")),
    Seq(ArticleIntroduction("This is an introduction", "en")),
    Seq(ArticleMetaDescription("meta", "en")),
    None,
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate,
    "ndalId54321",
    ArticleType.Standard.toString)

  val sampleDomainArticle = Article(
    Option(articleId),
    Option(2),
    Seq(ArticleTitle("title", "nb")),
    Seq(ArticleContent("content", "nb")),
    Copyright("by", "", Seq(), Seq(), Seq(), None, None, None),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    Seq(ArticleMetaDescription("meta description", "nb")),
    Some("11"),
    today,
    today,
    "ndalId54321",
    ArticleType.Standard.toString
  )

  val sampleDomainArticle2 = Article(
    None,
    None,
    Seq(ArticleTitle("test", "en")),
    Seq(ArticleContent("<article><div>test</div></article>", "en")),
    Copyright("publicdomain", "", Seq(), Seq(), Seq(), None, None, None),
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

  val newArticleV2 = api.NewArticleV2(
    "test",
    "<article><div>test</div></article>",
    Seq(),
    None,
    None,
    None,
    None,
    api.Copyright(api.License("publicdomain", None, None), "", Seq(), Seq(), Seq(), None, None, None),
    None,
    "standard",
    "en"
  )

  val newArticleV2Body = api.NewArticleV2(
    "title",
    "content",
    Seq("tag"),
    Some("introductino"),
    Some("metadescription"),
    Some("22"),
    None,
    api.Copyright(api.License("by-sa", None, None), "fromSomeWhere", Seq(api.Author("string", "du")), Seq(), Seq(), None, None, None),
    None,
    "standard",
    "nb"
  )

  val updatedArticleV2 = api.UpdatedArticleV2(
    1,
    "nb",
    Some("updated title"),
    None,
    Seq.empty,
    None,
    None,
    None,
    None,
    None,
    Seq.empty,
    None
  )

  val sampleArticleWithByNcSa = sampleArticleWithPublicDomain.copy(copyright=byNcSaCopyright)
  val sampleArticleWithCopyrighted = sampleArticleWithPublicDomain.copy(copyright=copyrighted )

  val sampleDomainArticleWithHtmlFault = Article(
    Option(articleId),
    Option(2),
    Seq(ArticleTitle("test", "en")),
    Seq(ArticleContent(
      """<ul><li><h1>Det er ikke lov å gjøre dette.</h1> Tekst utenfor.</li><li>Dette er helt ok</li></ul>
        |<ul><li><h2>Det er ikke lov å gjøre dette.</h2></li><li>Dette er helt ok</li></ul>
        |<ol><li><h3>Det er ikke lov å gjøre dette.</h3></li><li>Dette er helt ok</li></ol>
        |<ol><li><h4>Det er ikke lov å gjøre dette.</h4></li><li>Dette er helt ok</li></ol>
      """.stripMargin, "en")),
    Copyright("publicdomain", "", Seq(), Seq(), Seq(), None, None, None),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    Seq(ArticleMetaDescription("meta description", "nb")),
    None,
    today,
    today,
    "ndalId54321",
    ArticleType.Standard.toString
  )

  val apiArticleWithHtmlFaultV2 = api.ArticleV2(
    1,
    None,
    1,
    api.ArticleTitle("test", "en"),
    api.ArticleContentV2(
      """<ul><li><h1>Det er ikke lov å gjøre dette.</h1> Tekst utenfor.</li><li>Dette er helt ok</li></ul>
        |<ul><li><h2>Det er ikke lov å gjøre dette.</h2></li><li>Dette er helt ok</li></ul>
        |<ol><li><h3>Det er ikke lov å gjøre dette.</h3></li><li>Dette er helt ok</li></ol>
        |<ol><li><h4>Det er ikke lov å gjøre dette.</h4></li><li>Dette er helt ok</li></ol>
      """.stripMargin, "en"),
    api.Copyright(api.License("publicdomain", None, None), "", Seq(), Seq(), Seq(), None, None, None),
    api.ArticleTag(Seq.empty, "en"),
    Seq.empty,
    None,
    None,
    None,
    api.ArticleMetaDescription("so meta", "en"),
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate,
    "ndalId54321",
    "standard",
    Seq("en")
  )

  val (nodeId, nodeId2) = ("1234", "4321")
  val sampleTitle = ArticleTitle("title", "en")
  val sampleContent = LanguageContent(nodeId, nodeId, "sample content", "metadescription", "en", None, "fagstoff", Some("title"), Seq.empty)
  val sampleTranslationContent = sampleContent.copy(tnid=nodeId2)

  val visualElement = VisualElement(s"""<$ResourceHtmlEmbedTag  data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" />""", "nb")

  val sampleImageMetaInformation = ImageMetaInformation(
    "1",
    List(ImageTitle("Sample title", Some("nb"))),
    List(ImageAltText("alt text", Some("nb"))),
    "://image-url.com/image/img.jpg",
    1024,
    "application/jpeg",
    ImageCopyright(ImageLicense("by", "Creative Commons", None), "pix", Seq.empty),
    ImageTag(Seq("sample tag"), Some("nb")))

  val sampleConcept = Concept(
    Some(1),
    Seq(ConceptTitle("Tittel for begrep", "nb")),
    Seq(ConceptContent("Innhold for begrep", "nb")),
    Some(Copyright("publicdomain", "", Seq(), Seq(), Seq(), None, None, None)),
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate
  )

  val sampleApiConcept = api.Concept(
    1,
    api.ConceptTitle("Tittel for begrep", "nb"),
    api.ConceptContent("Innhold for begrep", "nb"),
    Some(api.Copyright(api.License("publicdomain", None, None), "", Seq(), Seq(), Seq(), None, None, None)),
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate,
    Set("nb")
  )

}


