/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import java.util.Date

import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.articleapi.integration._
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.api
import no.ndla.articleapi.service.converters.{Attributes, ResourceType, TableConverter}
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import org.mockito.Mockito._
import scala.util.{Success, Try}

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  val service = new ConverterService
  val contentTitle = ArticleTitle("", Some(""))
  val author = Author("forfatter", "Henrik")
  val tag = ArticleTag(List("asdf"), Some("nb"))
  val requiredLibrary = RequiredLibrary("", "", "")
  val nodeId = "1234"
  val sampleAlt = "Fotografi"
  val sampleContentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$sampleAlt==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val sampleNode = NodeToConvert(List(contentTitle), Seq(), "by-sa", Seq(author), List(tag), "fagstoff", "fagstoff", new Date(0), new Date(1), ArticleType.Standard)
  val sampleLanguageContent = TestData.sampleContent.copy(content=sampleContentString, language=Some("nb"))

  test("That the document is wrapped in an article tag") {
    val nodeId = "1"
    val initialContent = "<h1>Heading</h1>"
    val contentNode = sampleLanguageContent.copy(content=initialContent)
    val node = sampleNode.copy(contents=List(contentNode))
    val expedtedResult = initialContent

    when(extractConvertStoreContent.processNode("4321")).thenReturn(Try(TestData.sampleArticleWithPublicDomain, ImportStatus(Seq(), Seq())))

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus(Seq(), Seq()))

    result.content.head.content should equal (expedtedResult)
  }

  test("That content embedded in a node is converted") {
    val (nodeId, nodeId2) = ("1234", "4321")
    val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=inline==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val contentString2 = s"[contentbrowser ==nid=$nodeId2==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=inline==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val sampleOppgave1 = NodeGeneralContent(nodeId, nodeId, "Tittel", s"Innhold! $contentString2", "nb")
    val sampleOppgave2 = NodeGeneralContent(nodeId, nodeId2, "Tittel", "Enda mer innhold!", "nb")
    val node = sampleNode.copy(contents=List(sampleLanguageContent.copy(content=contentString)))

    when(extractService.getNodeType(nodeId)).thenReturn(Some("oppgave"))
    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleOppgave1))

    when(extractService.getNodeType(nodeId2)).thenReturn(Some("oppgave"))
    when(extractService.getNodeGeneralContent(nodeId2)).thenReturn(Seq(sampleOppgave2))

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus(Seq(), Seq()))
    result.content.head.content should equal ("Innhold! Enda mer innhold!")
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That the ingress is not added to the content") {
    val (nodeId, nodeId2) = ("1234", "4321")
    val ingressNodeBokmal = LanguageIngress("Hvem er sterkest?", None)
    val contentNodeBokmal = TestData.sampleContent.copy(content="Nordavinden og sola kranglet en gang om hvem av dem som var den sterkeste", ingress=Some(ingressNodeBokmal))

    val ingressNodeNynorsk = LanguageIngress("Kven er sterkast?", None)
    val contentNodeNynorsk = TestData.sampleContent.copy(content="Nordavinden og sola krangla ein gong om kven av dei som var den sterkaste", ingress=Some(ingressNodeNynorsk))

    val node = sampleNode.copy(contents=List(contentNodeBokmal, contentNodeNynorsk))
    val bokmalExpectedResult = "Nordavinden og sola kranglet en gang om hvem av dem som var den sterkeste"
    val nynorskExpectedResult = "Nordavinden og sola krangla ein gong om kven av dei som var den sterkaste"

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus(Seq(), Seq()))
    val bokmalResult = result.content.head.content
    val nynorskResult = result.content.last.content

    bokmalResult should equal (bokmalExpectedResult)
    nynorskResult should equal (nynorskExpectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("ingress is extracted when wrapped in <p> tags") {
    val content =
      s"""<section>
        |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5359" data-align="" data-resource="image" data-alt="To personer" data-caption="capt.">
        |<p><strong>Når man driver med medieproduksjon, er det mye arbeid som må gjøres<br></strong></p>
        |</section>
        |<section> <p>Det som kan gi helse- og sikkerhetsproblemer på en dataarbeidsplass, er:</section>""".stripMargin.replace("\n", "")
    val expectedContentResult = ArticleContent(
      s"""<section>
         |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5359" data-align="" data-resource="image" data-alt="To personer" data-caption="capt.">
         |<p><strong>Når man driver med medieproduksjon, er det mye arbeid som må gjøres<br></strong></p>
         |</section>
         |<section> <p>Det som kan gi helse- og sikkerhetsproblemer på en dataarbeidsplass, er:</p></section>""".stripMargin.replace("\n", ""), None, Some("nb"))

    val expectedIngressResult = ArticleIntroduction("Hvem er sterkest?", Some("nb"))

    val ingressNodeBokmal = LanguageIngress("Hvem er sterkest?", None)
    val contentNodeBokmal = sampleLanguageContent.copy(content=content, ingress=Some(ingressNodeBokmal))

    val node = sampleNode.copy(contents=List(contentNodeBokmal))
    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus(Seq(), Seq()))

    result.content.length should be (1)
    result.introduction.length should be (1)
    result.content.head should equal(expectedContentResult)
    result.introduction.head should equal(expectedIngressResult)
  }

  test("That html attributes are removed from the article") {
    val contentNodeBokmal = sampleLanguageContent.copy(content="""<table class="testclass" title="test"></table>""")
    val node = sampleNode.copy(contents=List(contentNodeBokmal))
    val bokmalExpectedResult = """<table></table>"""

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus(Seq(), Seq()))

    result.content.head.content should equal (bokmalExpectedResult)
    status.messages.nonEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That align attributes for td tags are not removed") {
    val htmlTableWithAlignAttributes = """<table><tbody><tr><td align="right" valign="top">Table row cell</td></tr></tbody></table>"""
    val contentNodeBokmal = sampleLanguageContent.copy(content=htmlTableWithAlignAttributes)
    val node = sampleNode.copy(contents=List(contentNodeBokmal))
    val expectedResult = """<table><tbody><tr><th align="right" valign="top">Table row cell</th></tr></tbody></table>"""

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus(Seq(), Seq()))

    result.content.head.content should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That html comments are removed") {
    val contentNodeBokmal = sampleLanguageContent.copy(content="""<p><!-- this is a comment -->not a comment</p><!-- another comment -->""")
    val node = sampleNode.copy(contents=List(contentNodeBokmal))
    val expectedResult = "<p>not a comment</p>"

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus(Seq(), Seq()))

    result.content.head.content should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That images are converted") {
    val (nodeId, imageUrl, alt) = ("1234", "full.jpeg", "Fotografi")
    val newId = "1"
    val contentNode = sampleLanguageContent.copy(content=s"<article>$sampleContentString</article>")
    val node = sampleNode.copy(contents=List(contentNode))
    val imageMeta = ImageMetaInformation(newId, List(), List(), imageUrl, 256, "", ImageCopyright(ImageLicense("", "", Some("")), "", List()), List())
    val expectedResult =
      s"""|<article>
          |<$resourceHtmlEmbedTag data-align="" data-alt="$sampleAlt" data-caption="" data-resource="image" data-resource_id="1" data-size="fullbredde">
          |</article>""".stripMargin.replace("\n", "")

    when(extractService.getNodeType(nodeId)).thenReturn(Some("image"))
    when(imageApiClient.importOrGetMetaByExternId(nodeId)).thenReturn(Some(imageMeta))
    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus(Seq(), Seq()))

    result.content.head.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("&nbsp is removed") {
    val contentNodeBokmal = sampleLanguageContent.copy(content="""<article> <p>hello&nbsp; you</article>""")
    val node = sampleNode.copy(contents=List(contentNodeBokmal))
    val expectedResult = """<article> <p>hello you</p></article>"""

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus(Seq(), Seq()))
    val strippedResult = " +".r.replaceAllIn(result.content.head.content.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That empty html tags are removed") {
    val contentNodeBokmal = sampleLanguageContent.copy(content=s"""<article> <div></div><p><div></div></p><$resourceHtmlEmbedTag ></$resourceHtmlEmbedTag></article>""")
    val node = sampleNode.copy(contents=List(contentNodeBokmal))
    val expectedResult = s"""<article> <$resourceHtmlEmbedTag></article>"""

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus(Seq(), Seq()))

    result.content.head.content should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("paragraphs are unwrapped if cell contains only one") {
    val table =
      s"""<table>
          |<tbody>
          |<tr>
          |<td><p>column</p></td>
          |</tr>
          |</tbody>
          |</table>""".stripMargin.replace("\n", "")

    val tableExpectedResult =
      s"""<table>
          |<tbody>
          |<tr>
          |<th>column</th>
          |</tr>
          |</tbody>
          |</table>""".stripMargin.replace("\n", "")

    val initialContent: LanguageContent = sampleLanguageContent.copy(content=table)
    val Success((content, _)) = TableConverter.convert(initialContent, ImportStatus(Seq(), Seq()))
    content.content should equal(tableExpectedResult)
  }

  test("MathML elements are converted correctly") {
    val originalContent = "<math><menclose notation=\"updiagonalstrike\"></menclose>\u00a0</math>"
    val expectedContent = """<math xmlns="http://www.w3.org/1998/Math/MathML"><menclose notation="updiagonalstrike"></menclose> </math>"""
    val initialContent: LanguageContent = sampleLanguageContent.copy(content=originalContent)
    val node = sampleNode.copy(contents=List(initialContent))

    val Success((content: Article, _)) = service.toDomainArticle(node, ImportStatus(Seq.empty, Seq.empty))

    content.content.head.content should equal(expectedContent)
  }

  test("JoubelH5PConverter is used when ENABLE_JOUBEL_H5P_OEMBED is true") {
    val h5pNodeId = "160303"
    val contentStringWithValidNodeId = s"[contentbrowser ==nid=$h5pNodeId==imagecache=Fullbredde==width===alt=$sampleAlt==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val expectedResult = s"""<$resourceHtmlEmbedTag data-resource="h5p" data-url="${JoubelH5PConverter.JoubelH5PBaseUrl}/1">"""

    val contentNodeBokmal = sampleLanguageContent.copy(content=contentStringWithValidNodeId)
    val node = sampleNode.copy(contents=List(contentNodeBokmal))

    when(extractService.getNodeType(h5pNodeId)).thenReturn(Some("h5p_content"))

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus(Seq(), Seq()))

    result.content.head.content should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("toDomainArticle convert a NewArticle to Article") {
    service.toDomainArticle(TestData.newArticle) should equal(
      TestData.sampleDomainArticle2.copy(created=clock.now, updated=clock.now, updatedBy=null)
    )
  }

  test("toDomainArticle convert a NewArticleV2 to Article") {
    service.toDomainArticle(TestData.newArticleV2) should equal(
      TestData.sampleDomainArticle2.copy(created=clock.now, updated=clock.now, updatedBy=null)
    )
  }

  test("toDomainArticle should return Failure if convertion fails") {
    val nodeId = 123123
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$sampleAlt==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val contentNodeBokmal = sampleLanguageContent.copy(content=contentString)
    val node = sampleNode.copy(contents=List(contentNodeBokmal))

    when(extractService.getNodeType(s"$nodeId")).thenReturn(Some("image"))
    when(imageApiClient.importOrGetMetaByExternId(s"$nodeId")).thenReturn(None)

    service.toDomainArticle(node, ImportStatus(Seq.empty, Seq.empty)).isFailure should be (true)
  }

  test("toApiLicense defaults to unknown if the license was not found") {
    service.toApiLicense("invalid") should equal(api.License("unknown", None, None))
  }

  test("toApiLicense converts a short license string to a license object with description and url") {
    service.toApiLicense("by") should equal(api.License("by", Some("Creative Commons Attribution 2.0 Generic"), Some("https://creativecommons.org/licenses/by/2.0/")))
  }

  test("toApiArticle converts a domain.Article to an api.Article") {
    when(articleRepository.getExternalIdFromId(TestData.articleId)).thenReturn(Some(TestData.externalId))
    service.toApiArticle(TestData.sampleDomainArticle) should equal(TestData.sampleArticle)
  }

  test("toApiArticleV2 converts a domain.Article to an api.ArticleV2") {
    when(articleRepository.getExternalIdFromId(TestData.articleId)).thenReturn(Some(TestData.externalId))
    service.toApiArticleV2(TestData.sampleDomainArticle, "nb") should equal(Some(TestData.apiArticleV2))
  }

  test("toApiArticleV2 returns None when language is not supported") {
    when(articleRepository.getExternalIdFromId(TestData.articleId)).thenReturn(Some(TestData.externalId))
    service.toApiArticleV2(TestData.sampleDomainArticle, "someRandomLanguage") should be(None)
    service.toApiArticleV2(TestData.sampleDomainArticle, "") should be(None)
  }

  test("toDomainArticleShould should remove unneeded attributes on embed-tags") {
    val content = s"""<h1>hello</h1><embed ${Attributes.DataResource}="${ResourceType.Image}" ${Attributes.DataUrl}="http://some-url" ${Attributes.DataId}=1 data-random="hehe" />"""
    val expectedContent = s"""<h1>hello</h1><embed ${Attributes.DataResource}="${ResourceType.Image}">"""
    val visualElement = s"""<embed ${Attributes.DataResource}="${ResourceType.Image}" ${Attributes.DataUrl}="http://some-url" ${Attributes.DataId}=1 data-random="hehe" />"""
    val expectedVisualElement = s"""<embed ${Attributes.DataResource}="${ResourceType.Image}">"""
    val articleContent = api.ArticleContent(content, None, Some("en"))
    val articleVisualElement = api.VisualElement(visualElement, Some("en"))
    val apiArticle = TestData.newArticle.copy(content=Seq(articleContent), visualElement=Some(Seq(articleVisualElement)))

    val result = service.toDomainArticle(apiArticle)
    result.content.head.content should equal (expectedContent)
    result.visualElement.head.resource should equal (expectedVisualElement)
  }

  test("VisualElement should be converted") {
    val node = sampleNode.copy(contents=List(TestData.sampleContent.copy(visualElement=Some(nodeId))))
    val expectedResult = s"""<$resourceHtmlEmbedTag data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" />"""
    when(extractService.getNodeType(nodeId)).thenReturn(Some("image"))
    when(imageApiClient.importOrGetMetaByExternId(nodeId)).thenReturn(Some(TestData.sampleImageMetaInformation))

    val Success((convertedArticle: Article, _)) = service.toDomainArticle(node, ImportStatus.empty)
    convertedArticle.visualElement should equal (Seq(VisualElement(expectedResult, Some("en"))))
  }

  test("That divs with class 'ndla_table' is converted to table") {
    val sampleLanguageContent: LanguageContent = TestData.sampleContent.copy(content="<article><div class=\"ndla_table another_class\">nobody builds walls better than me, believe me</div></article>")
    val node = sampleNode.copy(contents=List(sampleLanguageContent))
    val expectedResult = "<article><table>nobody builds walls better than me, believe me</table></article>"
    val result = service.toDomainArticle(node, ImportStatus.empty)
    result.isSuccess should be (true)

    val Success((content: Article, _)) = result

    content.content.head.content should equal (expectedResult)
    content.requiredLibraries.length should equal (0)
  }

  test("Concepts should only contain plain text") {
    val sampleLanguageContent: LanguageContent = TestData.sampleContent.copy(content="<h1>Nobody builds walls better than <strong>me, believe me</strong></h1>")
    val node = sampleNode.copy(contents=List(sampleLanguageContent), nodeType="begrep")
    val expectedResult = "Nobody builds walls better than me, believe me"

    val Success((result: Concept, _)) = service.toDomainArticle(node, ImportStatus.empty)

    result.content.head.content should equal(expectedResult)
  }
}
