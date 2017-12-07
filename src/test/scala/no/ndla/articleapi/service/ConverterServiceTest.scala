/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import java.util.Date

import no.ndla.articleapi.integration._
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.ImportException
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.service.converters.TableConverter
import no.ndla.validation.{ResourceType, TagAttributes}
import no.ndla.validation.EmbedTagRules.ResourceHtmlEmbedTag
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito._
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.util.{Success, Try}

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  val service = new ConverterService
  val contentTitle = ArticleTitle("", "unknown")
  val author = Author("forfatter", "Henrik")
  val tag = ArticleTag(List("asdf"), "nb")
  val requiredLibrary = RequiredLibrary("", "", "")
  val nodeId = "1234"
  val sampleAlt = "Fotografi"
  val sampleContentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$sampleAlt==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val sampleNode = NodeToConvert(List(contentTitle), Seq(), "by-sa", Seq(author), List(tag), "fagstoff", "fagstoff", new Date(0), new Date(1), ArticleType.Standard)
  val sampleLanguageContent = TestData.sampleContent.copy(content=sampleContentString, language="nb")

  test("That the document is wrapped in an section tag") {
    val nodeId = "1"
    val initialContent = "<h1>Heading</h1>"
    val contentNode = sampleLanguageContent.copy(content=initialContent)
    val node = sampleNode.copy(contents=List(contentNode))
    val expedtedResult = s"<section>$initialContent</section>"

    when(extractConvertStoreContent.processNode("4321")).thenReturn(Try(TestData.sampleArticleWithPublicDomain, ImportStatus.empty))

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus.empty)

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

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus.empty)
    result.content.head.content should equal ("<section>Innhold! Enda mer innhold!</section>")
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
    val bokmalExpectedResult = "<section>Nordavinden og sola kranglet en gang om hvem av dem som var den sterkeste</section>"
    val nynorskExpectedResult = "<section>Nordavinden og sola krangla ein gong om kven av dei som var den sterkaste</section>"

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus.empty)
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
        |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5359" data-align="" data-resource="image" data-alt="To personer" data-caption="capt.">
        |<p><strong>Når man driver med medieproduksjon, er det mye arbeid som må gjøres<br></strong></p>
        |</section>
        |<section> <p>Det som kan gi helse- og sikkerhetsproblemer på en dataarbeidsplass, er:</section>""".stripMargin.replace("\n", "")
    val expectedContentResult = ArticleContent(
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5359" data-align="" data-resource="image" data-alt="To personer" data-caption="capt.">
         |<p><strong>Når man driver med medieproduksjon, er det mye arbeid som må gjøres<br></strong></p>
         |</section>
         |<section><p>Det som kan gi helse- og sikkerhetsproblemer på en dataarbeidsplass, er:</p></section>""".stripMargin.replace("\n", ""), "nb")

    val expectedIngressResult = ArticleIntroduction("Hvem er sterkest?", "nb")

    val ingressNodeBokmal = LanguageIngress("Hvem er sterkest?", None)
    val contentNodeBokmal = sampleLanguageContent.copy(content=content, ingress=Some(ingressNodeBokmal))

    val node = sampleNode.copy(contents=List(contentNodeBokmal))
    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus.empty)

    result.content.length should be (1)
    result.introduction.length should be (1)
    result.content.head should equal(expectedContentResult)
    result.introduction.head should equal(expectedIngressResult)
  }

  test("That html attributes are removed from the article") {
    val contentNodeBokmal = sampleLanguageContent.copy(content="""<section><div class="testclass" title="test">high</div></section>""")
    val node = sampleNode.copy(contents=List(contentNodeBokmal))
    val bokmalExpectedResult = """<section><div>high</div></section>"""

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus.empty)

    result.content.head.content should equal (bokmalExpectedResult)
    status.messages.nonEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That align attributes for td tags are not removed") {
    val htmlTableWithAlignAttributes = """<section><table><tbody><tr><td align="right" valign="top">Table row cell</td></tr></tbody></table></section>"""
    val contentNodeBokmal = sampleLanguageContent.copy(content=htmlTableWithAlignAttributes)
    val node = sampleNode.copy(contents=List(contentNodeBokmal))
    val expectedResult = """<section><table><tbody><tr><td align="right" valign="top">Table row cell</td></tr></tbody></table></section>"""

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus.empty)

    result.content.head.content should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That html comments are removed") {
    val contentNodeBokmal = sampleLanguageContent.copy(content="""<section><p><!-- this is a comment -->not a comment</p><!-- another comment --></section>""")
    val node = sampleNode.copy(contents=List(contentNodeBokmal))
    val expectedResult = "<section><p>not a comment</p></section>"

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus.empty)

    result.content.head.content should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That images are converted") {
    val (nodeId, imageUrl, alt) = ("1234", "full.jpeg", "Fotografi")
    val newId = "1"
    val contentNode = sampleLanguageContent.copy(content=s"<section>$sampleContentString</section>")
    val node = sampleNode.copy(contents=List(contentNode))
    val imageMeta = ImageMetaInformation(newId, List(), List(), imageUrl, 256, "", ImageCopyright(ImageLicense("", "", Some("")), "", List()), ImageTag(List(), None))
    val expectedResult =
      s"""|<section>
          |<$ResourceHtmlEmbedTag data-align="" data-alt="$sampleAlt" data-caption="" data-resource="image" data-resource_id="1" data-size="fullbredde">
          |</section>""".stripMargin.replace("\n", "")

    when(extractService.getNodeType(nodeId)).thenReturn(Some("image"))
    when(imageApiClient.importImage(nodeId)).thenReturn(Some(imageMeta))
    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus.empty)

    result.content.head.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("&nbsp is removed") {
    val contentNodeBokmal = sampleLanguageContent.copy(content="""<section> <p>hello&nbsp; you</section>""")
    val node = sampleNode.copy(contents=List(contentNodeBokmal))
    val expectedResult = """<section><p>hello you</p></section>"""

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus.empty)
    val strippedResult = " +".r.replaceAllIn(result.content.head.content.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That empty html tags are removed") {
    val contentNodeBokmal = sampleLanguageContent.copy(content=s"""<section> <div></div><p><div></div></p><$ResourceHtmlEmbedTag ></$ResourceHtmlEmbedTag></section>""")
    val node = sampleNode.copy(contents=List(contentNodeBokmal))
    val expectedResult = s"""<section><$ResourceHtmlEmbedTag></section>"""

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus.empty)

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
          |<td>column</td>
          |</tr>
          |</tbody>
          |</table>""".stripMargin.replace("\n", "")

    val initialContent: LanguageContent = sampleLanguageContent.copy(content=table)
    val Success((content, _)) = TableConverter.convert(initialContent, ImportStatus.empty)
    content.content should equal(tableExpectedResult)
  }

  test("MathML elements are converted correctly") {
    val originalContent = "<section><math><menclose notation=\"updiagonalstrike\"></menclose>\u00a0</math></section>"
    val expectedContent = """<section><p><math xmlns="http://www.w3.org/1998/Math/MathML"><menclose notation="updiagonalstrike"></menclose> </math></p></section>"""
    val initialContent: LanguageContent = sampleLanguageContent.copy(content=originalContent)
    val node = sampleNode.copy(contents=List(initialContent))

    val Success((content: Article, _)) = service.toDomainArticle(node, ImportStatus.empty)

    content.content.head.content should equal(expectedContent)
  }

  test("JoubelH5PConverter is used when ENABLE_JOUBEL_H5P_OEMBED is true") {
    val h5pNodeId = "160303"
    val contentStringWithValidNodeId = s"[contentbrowser ==nid=$h5pNodeId==imagecache=Fullbredde==width===alt=$sampleAlt==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val expectedResult = s"""<section><$ResourceHtmlEmbedTag data-resource="h5p" data-url="${JoubelH5PConverter.JoubelH5PBaseUrl}/1"></section>"""

    val contentNodeBokmal = sampleLanguageContent.copy(content=contentStringWithValidNodeId)
    val node = sampleNode.copy(contents=List(contentNodeBokmal))

    when(extractService.getNodeType(h5pNodeId)).thenReturn(Some("h5p_content"))

    val Success((result: Article, status)) = service.toDomainArticle(node, ImportStatus.empty)

    result.content.head.content should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
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
    when(imageApiClient.importImage(s"$nodeId")).thenReturn(None)

    service.toDomainArticle(node, ImportStatus.empty).isFailure should be (true)
  }

  test("toApiLicense defaults to unknown if the license was not found") {
    service.toApiLicense("invalid") should equal(api.License("unknown", None, None))
  }

  test("toApiLicense converts a short license string to a license object with description and url") {
    service.toApiLicense("by") should equal(api.License("by", Some("Creative Commons Attribution 2.0 Generic"), Some("https://creativecommons.org/licenses/by/2.0/")))
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

  test("toApiArticleV2 converts a domain.Article to an api.ArticleV2 with Agreement Copyright") {
    when(articleRepository.getExternalIdFromId(TestData.articleId)).thenReturn(Some(TestData.externalId))
    val from = DateTime.now().minusDays(5).toDate()
    val to = DateTime.now().plusDays(10).toDate()
    val agreementCopyright = api.Copyright(
      api.License("gnu", Some("gpl"), None),
      "http://tjohei.com/",
      List(),
      List(),
      List(api.Author("Supplier", "Mads LakseService")),
      None,
      Some(from),
      Some(to)
    )
    when(draftApiClient.getAgreementCopyright(1)).thenReturn(Some(agreementCopyright))

    val apiArticle = service.toApiArticleV2(TestData.sampleDomainArticle.copy(copyright = TestData.sampleDomainArticle.copyright.copy(
      processors = List(Author("Idea", "Kaptein Snabelfant")),
      rightsholders = List(Author("Publisher", "KjeksOgKakerAS")),
      agreementId = Some(1)
    )), "nb")

    apiArticle.get.copyright.creators.size should equal(0)
    apiArticle.get.copyright.processors.head.name should equal("Kaptein Snabelfant")
    apiArticle.get.copyright.rightsholders.head.name should equal("Mads LakseService")
    apiArticle.get.copyright.rightsholders.size should equal(1)
    apiArticle.get.copyright.license.license should equal("gnu")
    apiArticle.get.copyright.validFrom.get should equal(from)
    apiArticle.get.copyright.validTo.get should equal(to)
  }

  test("toDomainArticleShould should remove unneeded attributes on embed-tags") {
    val content = s"""<h1>hello</h1><embed ${TagAttributes.DataResource}="${ResourceType.Image}" ${TagAttributes.DataUrl}="http://some-url" data-random="hehe" />"""
    val expectedContent = s"""<h1>hello</h1><embed ${TagAttributes.DataResource}="${ResourceType.Image}">"""
    val visualElement = s"""<embed ${TagAttributes.DataResource}="${ResourceType.Image}" ${TagAttributes.DataUrl}="http://some-url" data-random="hehe" />"""
    val expectedVisualElement = s"""<embed ${TagAttributes.DataResource}="${ResourceType.Image}">"""
    val apiArticle = TestData.newArticleV2.copy(content=content, visualElement=Some(visualElement))

    val result = service.toDomainArticle(apiArticle)
    result.content.head.content should equal (expectedContent)
    result.visualElement.head.resource should equal (expectedVisualElement)
  }

  test("VisualElement should be converted") {
    val node = sampleNode.copy(contents=List(TestData.sampleContent.copy(visualElement=Some(nodeId))))
    val expectedResult = s"""<$ResourceHtmlEmbedTag data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" />"""
    when(extractService.getNodeType(nodeId)).thenReturn(Some("image"))
    when(imageApiClient.importImage(nodeId)).thenReturn(Some(TestData.sampleImageMetaInformation))

    val Success((convertedArticle: Article, _)) = service.toDomainArticle(node, ImportStatus.empty)
    convertedArticle.visualElement should equal (Seq(VisualElement(expectedResult, "en")))
  }

  test("That divs with class 'ndla_table' is converted to table") {
    val sampleLanguageContent: LanguageContent = TestData.sampleContent.copy(content="<section><div class=\"ndla_table another_class\">nobody builds walls better than me, believe me</div></section>")
    val node = sampleNode.copy(contents=List(sampleLanguageContent))
    val expectedResult = "<section><table>nobody builds walls better than me, believe me</table></section>"
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

  test("toDomainArticle should only include tags in relevant languages") {
    val titles = Seq(ArticleTitle("tiitel", "nb"))
    val contents = Seq(TestData.sampleContent.copy(language="nb"))
    val tags = Seq(ArticleTag(Seq("t1", "t2"), "nb"), ArticleTag(Seq("t1", "t2"), "en"))

    val node = sampleNode.copy(titles=titles, contents=contents, tags=tags)
    service.toDomainArticle(node).tags.map(_.language) should equal(Seq("nb"))
  }

  test("Leaf node converter should create an article from a pure h5p node") {
    val sampleLanguageContent = TestData.sampleContent.copy(content="<div><h1>hi</h1></div>", nodeType="h5p_content")
    val expectedResult = s"""<section><$ResourceHtmlEmbedTag data-resource="h5p" data-url="//ndla.no/h5p/embed/1234">${sampleLanguageContent.content}</section>"""
    val node = sampleNode.copy(contents=Seq(sampleLanguageContent), nodeType="h5p_content", contentType="oppgave")

    val Success((result: Article, _)) = service.toDomainArticle(node, ImportStatus.empty)

    result.content.head.content should equal (expectedResult)
    result.requiredLibraries.size should equal (1)
  }

  test("nbsp in MathML tags should be converted to space") {

    val sampleLanguageContent: LanguageContent = TestData.sampleContent.copy(content="<section><p><math>\u00a0<mi>P\u00a0</mi></math></p></section>")
    val expectedResult = """<section><p><math xmlns="http://www.w3.org/1998/Math/MathML"> <mi>P </mi></math></p></section>"""
    val node = sampleNode.copy(contents=List(sampleLanguageContent))

    val result = service.toDomainArticle(node, ImportStatus.empty)
    result.isSuccess should be (true)

    val Success((content: Article, _)) = result

    content.content.head.content should equal (expectedResult)
  }

  test("nbsp in MathML <mo> tags should not be converted to space if only nbsp") {
    val sampleLanguageContent: LanguageContent = TestData.sampleContent.copy(content="<section><p><math>\u00a0<mo>\u00a0</mo></math></p></section>")
    val expectedResult = "<section><p><math xmlns=\"http://www.w3.org/1998/Math/MathML\"> <mo>&#xa0;</mo></math></p></section>"
    val node = sampleNode.copy(contents=List(sampleLanguageContent))

    val result = service.toDomainArticle(node, ImportStatus.empty)
    result.isSuccess should be (true)

    val Success((content: Article, _)) = result

    content.content.head.content should equal (expectedResult)
  }

  test("That oldToNewLicenseKey throws on invalid license") {
    assertThrows[ImportException] {
      service.oldToNewLicenseKey("publicdomain")
    }
  }

  test("That oldToNewLicenseKey converts correctly") {
    service.oldToNewLicenseKey("nolaw") should be("cc0")
    service.oldToNewLicenseKey("noc") should be("pd")
  }

  test("That oldToNewLicenseKey does not convert an license that should not be converted") {
    service.oldToNewLicenseKey("by-sa") should be("by-sa")
  }

  test("That language of hit is returned correctly from getLanguageFromHit") {
    implicit val formats = DefaultFormats
    val jsonstring = """{"took":3,"timed_out":false,"_shards":{"total":5,"successful":5,"failed":0},"hits":{"total":3,"max_score":3.3036344,"hits":[{"_index":"articles_20171206110732","_type":"article","_id":"4","_score":3.3036344,"_source":{"id":4,"title":{"nb":"8. mars, den internasjonale kvinnedagen","nn":"8. mars, den internasjonale kvinnedagen"},"content":{"nb":"Det norske Kvinneforbundet var i første runde ikke interessert i å markere en egen kvinnedag, de ville heller arbeide parallelt med sine menn. En av grunnene var kanskje at en i Norge var i ferd med å nå målet om stemmerett for kvinner. Men i 1915, under første verdenskrig, arrangerte Kvinneforbundet i Arbeiderpartiet et stort folkemøte for fred. Kvinnedagen ble brukt til protester mot krigen, og feiringa av 8. mars dette året var relativt stor. Den eksakte datoen for feiringa av kvinnedagen hadde tidligere variert noe, men fra 1917 ble datoen endelig bestemt. Russiske kvinner demonstrerte denne dagen. Demonstrasjonen ble opptakten til den russiske revolusjonen, som førte til at tsaren ble styrtet. I 1922 bestemte Lenin at 8. mars skulle være en kommunistisk festdag. Markeringer av dagen var knyttet opp mot venstresiden i norsk politikk; Arbeiderpartiet og Norges kommunistiske parti. Den første offisielle 8. mars-feiringa etter krigen var i 1948. På 50-tallet, under den kalde krigen, ble dagen brukt til fredsdemonstrasjoner. Kvinnene som markerte dagen, ble møtt med mye motstand. I 1972 ble tradisjonen med å markere kvinnedagen tatt opp igjen. Det var Kvinnefronten i Oslo som tok initiativet til markering av dagen og inviterte andre kvinneorganisasjoner til å delta. FN erklærte 8. mars som internasjonal kvinnedag i forbindelse med det internasjonale kvinneåret i 1975. Den største markeringa på 70-tallet fant sted tre år senere, da 20 000 gikk i tog. Siden har det vært arrangert 8. mars-tog årlig i de største byene i Norge.","nn":"Det norske Kvinneforbundet var i første runde ikkje interessert i å markere ein eigen kvinnedag. Dei ville heller arbeide parallelt med mennene sine. Ein av grunnane var kanskje at ein i Noreg var i ferd med å nå målet om røysterett for kvinner. Men i 1915, under første verdskrigen, arrangerte Kvinneforbundet i Arbeidarpartiet eit stort folkemøte for fred. Kvinnedagen vart brukt til protestar mot krigen, og feiringa av 8. mars dette året var relativt stor. Den eksakte datoen for feiringa av kvinnedagen hadde tidlegare variert noko, men frå 1917 vart datoen endeleg fastsett. Russiske kvinner demonstrerte denne dagen. Demonstrasjonen vart opptakten til den russiske revolusjonen, som førde til at tsaren vart styrta. I 1922 avgjorde Lenin at 8. mars skulle vere ein kommunistisk festdag. Markeringa av dagen var knytt opp mot venstresida i norsk politikk: Arbeidarpartiet og Noregs kommunistiske parti. Den første offisielle 8. mars-feiringa etter krigen var i 1948. På 50-talet, under den kalde krigen, vart dagen brukt til fredsdemonstrasjonar. Kvinnene som markerte dagen, vart møtte med mykje motstand. I 1972 vart tradisjonen med å markere kvinnedagen teken opp igjen. Det var Kvinnefronten i Oslo som tok initiativet til markering av dagen og inviterte andre kvinneorganisasjonar til å delta. FN erklærte 8. mars som internasjonal kvinnedag i samband med det internasjonale kvinneåret i 1975. Den største markeringa på 70-talet fann stad tre år seinare, da 20 000 gjekk i tog. Sidan har det vore arrangert 8. mars-tog årleg i dei største byane i Noreg."},"visualElement":{},"introduction":{"nb":"8. mars er den internasjonale kvinnedagen. I forbindelse med at det ble kjempet for at kvinner skulle få stemmerett i USA, ble 8. mars for første gang markert som en nasjonal kvinnedag i New York i 1908. To år senere ble dagen vedtatt som en internasjonal kvinnedag på den andre internasjonale sosialdemokratiske kvinnekongressen i København i 1910.","nn":"8. mars er den internasjonale kvinnedagen. I samband med at det vart kjempa for at kvinner skulle få røysterett i USA, vart 8. mars for første gongen markert som ein nasjonal kvinnedag i New York i 1908. To år seinare vart dagen vedteken som internasjonal kvinnedag på den andre internasjonale sosialdemokratiske kvinnekongressen, i København i 1910."},"tags":{"nn":["8. mars","demokrati","likestilling","røysterett"],"nb":["8. mars","demokrati","kjønnskamp","kvinnedag","likestilling","stemmerett"]},"lastUpdated":"2017-06-20T07:13:45Z","license":"by-sa","authors":["Kristin Klepp"],"articleType":"standard"},"inner_hits":{"title":{"hits":{"total":0,"max_score":null,"hits":[]}},"introduction":{"hits":{"total":1,"max_score":1.0077262,"hits":[{"_nested":{"field":"introduction","offset":0},"_score":1.0077262,"_source":{"nb":"8. mars er den internasjonale kvinnedagen. I forbindelse med at det ble kjempet for at kvinner skulle få stemmerett i USA, ble 8. mars for første gang markert som en nasjonal kvinnedag i New York i 1908. To år senere ble dagen vedtatt som en internasjonal kvinnedag på den andre internasjonale sosialdemokratiske kvinnekongressen i København i 1910.","nn":"8. mars er den internasjonale kvinnedagen. I samband med at det vart kjempa for at kvinner skulle få røysterett i USA, vart 8. mars for første gongen markert som ein nasjonal kvinnedag i New York i 1908. To år seinare vart dagen vedteken som internasjonal kvinnedag på den andre internasjonale sosialdemokratiske kvinnekongressen, i København i 1910."},"highlight":{"introduction.nn":["8. mars er den internasjonale kvinnedagen. I samband med at det vart kjempa for at kvinner skulle få røysterett i USA, vart 8. mars for første gongen markert som ein nasjonal kvinnedag i New York i 1908. To år seinare vart dagen vedteken som internasjonal kvinnedag på den andre internasjonale sosialdemokratiske kvinnekongressen, i København i 1910."]}}]}},"content":{"hits":{"total":1,"max_score":1.288182,"hits":[{"_nested":{"field":"content","offset":0},"_score":1.288182,"_source":{"nb":"Det norske Kvinneforbundet var i første runde ikke interessert i å markere en egen kvinnedag, de ville heller arbeide parallelt med sine menn. En av grunnene var kanskje at en i Norge var i ferd med å nå målet om stemmerett for kvinner. Men i 1915, under første verdenskrig, arrangerte Kvinneforbundet i Arbeiderpartiet et stort folkemøte for fred. Kvinnedagen ble brukt til protester mot krigen, og feiringa av 8. mars dette året var relativt stor. Den eksakte datoen for feiringa av kvinnedagen hadde tidligere variert noe, men fra 1917 ble datoen endelig bestemt. Russiske kvinner demonstrerte denne dagen. Demonstrasjonen ble opptakten til den russiske revolusjonen, som førte til at tsaren ble styrtet. I 1922 bestemte Lenin at 8. mars skulle være en kommunistisk festdag. Markeringer av dagen var knyttet opp mot venstresiden i norsk politikk; Arbeiderpartiet og Norges kommunistiske parti. Den første offisielle 8. mars-feiringa etter krigen var i 1948. På 50-tallet, under den kalde krigen, ble dagen brukt til fredsdemonstrasjoner. Kvinnene som markerte dagen, ble møtt med mye motstand. I 1972 ble tradisjonen med å markere kvinnedagen tatt opp igjen. Det var Kvinnefronten i Oslo som tok initiativet til markering av dagen og inviterte andre kvinneorganisasjoner til å delta. FN erklærte 8. mars som internasjonal kvinnedag i forbindelse med det internasjonale kvinneåret i 1975. Den største markeringa på 70-tallet fant sted tre år senere, da 20 000 gikk i tog. Siden har det vært arrangert 8. mars-tog årlig i de største byene i Norge.","nn":"Det norske Kvinneforbundet var i første runde ikkje interessert i å markere ein eigen kvinnedag. Dei ville heller arbeide parallelt med mennene sine. Ein av grunnane var kanskje at ein i Noreg var i ferd med å nå målet om røysterett for kvinner. Men i 1915, under første verdskrigen, arrangerte Kvinneforbundet i Arbeidarpartiet eit stort folkemøte for fred. Kvinnedagen vart brukt til protestar mot krigen, og feiringa av 8. mars dette året var relativt stor. Den eksakte datoen for feiringa av kvinnedagen hadde tidlegare variert noko, men frå 1917 vart datoen endeleg fastsett. Russiske kvinner demonstrerte denne dagen. Demonstrasjonen vart opptakten til den russiske revolusjonen, som førde til at tsaren vart styrta. I 1922 avgjorde Lenin at 8. mars skulle vere ein kommunistisk festdag. Markeringa av dagen var knytt opp mot venstresida i norsk politikk: Arbeidarpartiet og Noregs kommunistiske parti. Den første offisielle 8. mars-feiringa etter krigen var i 1948. På 50-talet, under den kalde krigen, vart dagen brukt til fredsdemonstrasjonar. Kvinnene som markerte dagen, vart møtte med mykje motstand. I 1972 vart tradisjonen med å markere kvinnedagen teken opp igjen. Det var Kvinnefronten i Oslo som tok initiativet til markering av dagen og inviterte andre kvinneorganisasjonar til å delta. FN erklærte 8. mars som internasjonal kvinnedag i samband med det internasjonale kvinneåret i 1975. Den største markeringa på 70-talet fann stad tre år seinare, da 20 000 gjekk i tog. Sidan har det vore arrangert 8. mars-tog årleg i dei største byane i Noreg."},"highlight":{"content.nn":["Det norske Kvinneforbundet var i første runde ikkje interessert i å markere ein eigen kvinnedag. Dei ville heller arbeide parallelt med mennene sine. Ein av grunnane var kanskje at ein i Noreg var i ferd med å nå målet om røysterett for kvinner. Men i 1915, under første verdskrigen, arrangerte Kvinneforbundet i Arbeidarpartiet eit stort folkemøte for fred. Kvinnedagen vart brukt til protestar mot krigen, og feiringa av 8. mars dette året var relativt stor. Den eksakte datoen for feiringa av kvinnedagen hadde tidlegare variert noko, men frå 1917 vart datoen endeleg fastsett. Russiske kvinner demonstrerte denne dagen. Demonstrasjonen vart opptakten til den russiske revolusjonen, som førde til at tsaren vart styrta. I 1922 avgjorde Lenin at 8. mars skulle vere ein kommunistisk festdag. Markeringa av dagen var knytt opp mot venstresida i norsk politikk: Arbeidarpartiet og Noregs kommunistiske parti. Den første offisielle 8. mars-feiringa etter krigen var i 1948. På 50-talet, under den kalde krigen, vart dagen brukt til fredsdemonstrasjonar. Kvinnene som markerte dagen, vart møtte med mykje motstand. I 1972 vart tradisjonen med å markere kvinnedagen teken opp igjen. Det var Kvinnefronten i Oslo som tok initiativet til markering av dagen og inviterte andre kvinneorganisasjonar til å delta. FN erklærte 8. mars som internasjonal kvinnedag i samband med det internasjonale kvinneåret i 1975. Den største markeringa på 70-talet fann stad tre år seinare, da 20 000 gjekk i tog. Sidan har det vore arrangert 8. mars-tog årleg i dei største byane i Noreg."]}}]}},"tags":{"hits":{"total":0,"max_score":null,"hits":[]}}}},{"_index":"articles_20171206110732","_type":"article","_id":"11","_score":2.4251919,"_source":{"id":11,"title":{"nb":"Streik","nn":"Streik"},"content":{"nb":"Hensikten med en streik er å tvinge fram en løsning på en tvist mellom arbeidstakerne og arbeidsgiveren som arbeidstakerne kan være fornøyd med. Streik er altså et pressmiddel, fordi nedlegging av arbeidet kan påvirke kunder og brukere og slik påføre arbeidsgiveren store økonomiske tap. I mars 2015 var for eksempel nesten alle flyavganger med flyselskapet Norwegian i Skandinavia avlyst på grunn av streik blant pilotene. Frontene var harde, og streiken fikk også stor oppmerksomhet i mediene. Arbeidsgiversiden har også et virkemiddel som likner på streik, nemlig lockout. Ved en lockout blir arbeidsplassen helt eller delvis stengt. Dette skjer gjerne samtidig med en pågående streik, slik at arbeidstakere som ikke er i streik, blir permitterte. På samme måte som streik er hensikten med et slikt virkemiddel å tvinge fram en løsning på konflikten, til egen fordel. Lockout benyttes sjeldnere enn streik. Regler om streik Retten til å streike reguleres av to lover: arbeidstvistloven av 1928 og tjenestetvistloven av 1985. Lovene sier noe om hvem som har lov til å streike, og når det er lov til å gå til streik. Det er forbudt å streike når en er gyldig, og når det kommer til tvister angående den gjeldende avtale. En tariffavtale er gyldig i en viss periode, en tariffperiode, og det er først når denne perioden er ute og man skal forhandle om en ny avtale, at arbeidstakerne har rett til å streike De fleste grupper i arbeidslivet har rett og lov til å streike. Men noen faller likevel utenfor dette. Det gjelder embetsmenn og militære.","nn":"Formålet med ein streik er å tvinge fram ei løysing på ein tvist mellom arbeidstakarane og arbeidsgivaren som arbeidstakarane kan vere fornøgde med. Streik er altså eit pressmiddel, fordi nedlegging av arbeidet kan påverke kundar og brukarar og slik påføre arbeidsgivaren store økonomiske tap. I mars 2015 var til dømes nesten alle flyavgangar med flyselskapet Norwegian i Skandinavia avlyste på grunn av at pilotane streika. Frontane var harde, og streiken fekk òg store oppslag i media. Arbeidsgiversiden har også et virkemiddel som likner på streik, nemlig lockout. Ved en lockout blir arbeidsplassen helt eller delvis stengt. Dette skjer gjerne samtidig med en pågående streik, slik at arbeidstakere som ikke er i streik, blir permitterte. På samme måte som streik er hensikten med et slikt virkemiddel å tvinge fram en løsning på konflikten, til egen fordel. Lockout benyttes sjeldnere enn streik. Reglar om streik Retten til å streike er regulert av to lover: arbeidstvistlova av 1928 og tenestetvistlova av 1958.5 Lovene seier noko om kven som har lov til å streike, og når det er lov til å gå til streik. Det er forbode å streike når ein er gyldig, og når det kjem til tvistar om den avtalen som gjeld. Ein tariffavtale er gyldig i ein viss periode, ein tariffperiode, og det er først når denne perioden er ute og ein skal forhandle om ein ny avtale, at arbeidstakarane har rett til å streike. Dei fleste gruppene i arbeidslivet har rett og lov til å streike. Men nokre fell likevel utanfor dette. Det gjeld embetsmenn og militære."},"visualElement":{},"introduction":{"nb":"Streik ble første gang tatt i bruk i Norge i forbindelse med bergverksdriften på 1600-tallet. Streik vil si at arbeidstakerne legger ned arbeidet. I dag er dette et lovlig virkemiddel i arbeidslivet, når det skjer i forbindelse med et tariffoppgjør.","nn":"Streik blei første gongen teke i bruk i Noreg i samband med bergverksdrifta på 1600-talet. Streik vil seie at arbeidstakarane legg ned arbeidet. I dag er dette eit lovleg verkemiddel i arbeidslivet, når det skjer i samband med eit tariffoppgjer."},"tags":{"nn":["arbeidsliv","konfliktar","lønnsforhandlingar","streik","tariffoppgjør"],"nb":["arbeidsliv","konflikter","lønnsforhandlinger","streik","tariffoppgjør"]},"lastUpdated":"2017-08-09T09:16:31Z","license":"by-sa","authors":["Inga Berntsen Rudi"],"articleType":"standard"},"inner_hits":{"title":{"hits":{"total":0,"max_score":null,"hits":[]}},"introduction":{"hits":{"total":1,"max_score":1.2125959,"hits":[{"_nested":{"field":"introduction","offset":0},"_score":1.2125959,"_source":{"nb":"Streik ble første gang tatt i bruk i Norge i forbindelse med bergverksdriften på 1600-tallet. Streik vil si at arbeidstakerne legger ned arbeidet. I dag er dette et lovlig virkemiddel i arbeidslivet, når det skjer i forbindelse med et tariffoppgjør.","nn":"Streik blei første gongen teke i bruk i Noreg i samband med bergverksdrifta på 1600-talet. Streik vil seie at arbeidstakarane legg ned arbeidet. I dag er dette eit lovleg verkemiddel i arbeidslivet, når det skjer i samband med eit tariffoppgjer."},"highlight":{"introduction.nn":["Streik blei første gongen teke i bruk i Noreg i samband med bergverksdrifta på 1600-talet. Streik vil seie at arbeidstakarane legg ned arbeidet. I dag er dette eit lovleg verkemiddel i arbeidslivet, når det skjer i samband med eit tariffoppgjer."]}}]}},"content":{"hits":{"total":0,"max_score":null,"hits":[]}},"tags":{"hits":{"total":0,"max_score":null,"hits":[]}}}},{"_index":"articles_20171206110732","_type":"article","_id":"1","_score":1.3002287,"_source":{"id":1,"title":{"nb":"Arrangerte ekteskap og tvangsekteskap","nn":"Arrangerte ekteskap og tvangsekteskap"},"content":{"nb":"Ifølge vår vestlige menneskerettighetsoppfatning er det ethvert menneskes rett fritt å kunne velge om man vil gifte seg, og hvem man vil gifte seg med. De fleste av oss tar denne valgfriheten for gitt, men dessverre gjelder ikke dette for alle. Selv i dagens Norge blir unge mennesker tvunget eller presset av sine foreldre til å gifte seg mot sin vilje, dette til tross for at lovverket sier noe annet. Det er oftest barn og unge som utsettes for tvangsekteskap, og eneste vei bort fra et uønsket ekteskap er å bryte med foreldrene og slekten. Et slikt brudd kan være et uoverkommelig skritt å ta for en mindreårig eller ung voksen. Myndighetene Myndighetene har gjennom ulike tiltak forsøkt å ta problemet med tvangsekteskap på alvor. I forbindelse med den nye handlingsplanen mot tvangsekteskap som kom i 2007, uttalte daværende arbeids- og inkluderingsminister Bjarne Håkon Hanssen: Vi kan ikke akseptere at unge jenter og gutter i vårt samfunn blir giftet bort mot sin vilje. Det er viktig at vi hjelper dem på deres premisser. Gjennom satsingen på minoritetsrådgivere og integreringsrådgivere bidrar vi nå til å bygge opp et støtte- og hjelpeapparat for ungdommene dette kan gjelde. Gjennom trygge og gode tiltak skal myndighetene tilby et hjelpeapparat der de som blir utsatt for tvangsekteskap, skal få den støtten de trenger. Hvorfor arrangerte ekteskap? Arrangerte ekteskap har sammenheng med et foreldet kjønnsrollemønster. Det finnes i kulturer der kvinner har en undertrykket stilling og hennes dydighet skal kontrolleres. Mannens ære er også en sentral verdi i denne praksisen. Ved å miste kontrollen over kvinnen føler mannen seg som en hanrei (en bedratt ektemann). På samme måte vil en datter som ikke følger foreldrenes råd, bringe skam over familien, da særlig far, brødre og andre mannlige familiemedlemmer. Tvangsekteskapet er den ytterliggående formen for arrangert ekteskap, og beslutninger tas uten at man rådfører seg med den ene eller begge partene som skal inngå ekteskap. Arrangerte ekteskap \u2013 fornuftsekteskap? Det er ikke lenge siden det var vanlig med arrangerte ekteskap også i Norge. Gjennom ekteskap kunne man sikre gode forbindelser mellom slekter. Kulturkollisjon I miljøer der arrangerte ekteskap praktiseres, argumenterer man for stabiliteten i slike ekteskap, i motsetning til den store skilsmissestatistikken for etniske nordmenns kjærlighetsekteskap. Man kan diskutere om det er stabiliteten i familiestrukturene som er det viktigste, eller om den enkeltes frihet og likestilling mellom kjønnene skal telle mer. Et eksempel fra rettsapparatet: \"I 2006 tok Høyesterett for første gang stilling til straffeutmålingen i en straffesak etter den nye bestemmelsen i § 222 andre ledd i straffeloven. En mann ble sammen med en av sine sønner dømt for å ha truet med vold og for å forsøke å tvinge sin eldste datter til å inngå ekteskap da hun var 17 år. I tingretten ble de to dømt til henholdsvis ti og åtte måneders fengsel. Saken ble anket til lagmannsretten, som økte straffen til henholdsvis ett år og ni måneder og ett år og fem måneder. Saken ble videre anket til Høyesterett. Høyesterett understreket at tvangsekteskap er en grov krenkelse av individets frihet og selvstendighet og nesten uten unntak også en grov krenkelse av råderetten over egen kropp. Straffen ble satt til fengsel i to år og seks måneder for faren og to år for sønnen.\" (Handlingsplan for tvangsekteskap, 2007 s. 11)","nn":"Ifølgje den oppfatninga av menneskerettar som vi har i Vesten, er det å fritt kunne velje om ein vil gifte seg, og kven ein vil gifte seg med, ein rett for kvart menneske. Dei fleste av oss reknar denne valfridomen for sjølvsagd, men diverre gjeld det ikkje for alle. Sjølv i Noreg i dag vert unge menneske tvungne eller pressa av foreldra sine til å gifte seg mot sin eigen vilje, trass i at lovverket seier noko anna. Det er oftast barn og unge som vert offer for tvangsekteskap, og den einaste vegen bort frå eit uønskt ekteskap er å bryte med foreldra og slekta. Eit slikt brott kan vere eit uoverkommeleg steg å ta for ein mindreårig eller ung vaksen. Styresmaktene Styresmaktene har gjennom ulike tiltak forsøkt å ta problemet med tvangsekteskap på alvor. I samband med den nye handlingsplanen mot tvangsekteskap som kom i 2007, ytra daverande arbeids- og inkluderingsminister Bjarne Håkon Hanssen: \"Vi kan ikke akseptere at unge jenter og gutter i vårt samfunn blir giftet bort mot sin vilje. Det er viktig at vi hjelper dem på deres premisser. Gjennom satsingen på minoritetsrådgivere og integreringsrådgivere bidrar vi nå til å bygge opp et støtte- og hjelpeapparat for ungdommene dette kan gjelde.\" Gjennom trygge og gode tiltak skal styresmaktene tilby eit hjelpeapparat der dei som opplever tvangsekteskap, skal få den støtta dei treng. Kvifor arrangerte ekteskap? Arrangerte ekteskap har samanheng med eit forelda kjønnsrollemønster. Det finst i kulturar der kvinner har ei undertrykt stilling og dydigheita hennar skal kontrollerast. Æra til mannen er også ein sentral verdi i denne praksisen. Om han mistar kontrollen over kvinna, føler mannen seg som ein hanrei (ein mann som kona har vore utru mot). På same måte vil ei dotter som ikkje følgjer familiens råd, bringe skam over familien, da spesielt far, brør og andre mannlege familiemedlemmer. Tvangsekteskapet er den ytterleggåande forma for arrangert ekteskap, og avgjerder vert fatta utan at ein rådfører seg med den eine av eller begge partane som skal inngå ekteskap. Arrangerte ekteskap \u2013 fornuftsekteskap? Det er ikkje lenge sidan det var vanleg med arrangerte ekteskap også i Noreg. Gjennom ekteskap kunne ein sikre gode alliansar mellom slekter. Kulturkollisjon I miljø der arrangerte ekteskap vert praktisert, argumenterer ein for stabiliteten i slike ekteskap, i motsetnad til den store skilsmissestatistikken for kjærleiksekteskapa til etniske nordmenn. Ein kan diskutere om det er stabiliteten i familiestrukturane som er det viktigaste, eller om fridomen til den einskilde og likestillinga mellom kjønna skal telje meir. Eit døme frå rettsapparatet: \"I 2006 tok Høyesterett for første gang stilling til straffeutmålingen i en straffesak etter den nye bestemmelsen i § 222 andre ledd i straffeloven. En mann ble sammen med en av sine sønner dømt for å ha truet med vold og for å forsøke å tvinge sin eldste datter til å inngå ekteskap da hun var 17 år. I tingretten ble de to dømt til henholdsvis ti og åtte måneders fengsel. Saken ble anket til lagmannsretten, som økte straffen til henholdsvis ett år og ni måneder og ett år og fem måneder. Saken ble videre anket til Høyesterett. Høyesterett understreket at tvangsekteskap er en grov krenkelse av individets frihet og selvstendighet og nesten uten unntak også en grov krenkelse av råderetten over egen kropp. Straffen ble satt til fengsel i to år og seks måneder for faren og to år for sønnen.\" (Handlingsplan for tvangsekteskap, 2007, s. 11)"},"visualElement":{},"introduction":{"nb":"Tvangsekteskap er klart ulovlig ifølge norsk lov. Det fratar enkeltindividet retten til selv å bestemme over sitt liv. Arrangerte ekteskap er kjent i mange samfunn og kulturer, og er ikke det samme som tvangsekteskap, selv om mange vil mene at de to formene for å finne ektemaker glir over i hverandre.","nn":"Tvangsekteskap er klart ulovleg etter norsk lov. Det tek frå einskildindividet retten til å rå over sitt eige liv. Arrangerte ekteskap kjenner vi til frå mange samfunn og kulturar og er ikkje det same som tvangsekteskap, sjølv om mange vil meine at dei to måtane å finne ektemakar på glid over i kvarandre."},"tags":{"nn":["ekteskap","lovgiving","tvangsekteskap"],"nb":["ekteskap","lovgivning","tvangsekteskap"]},"lastUpdated":"2017-03-03T16:38:33Z","license":"by-nc-sa","authors":["Hans Nissen","Hans Nissen"],"articleType":"standard"},"inner_hits":{"title":{"hits":{"total":0,"max_score":null,"hits":[]}},"introduction":{"hits":{"total":0,"max_score":null,"hits":[]}},"content":{"hits":{"total":1,"max_score":1.3002287,"hits":[{"_nested":{"field":"content","offset":0},"_score":1.3002287,"_source":{"nb":"Ifølge vår vestlige menneskerettighetsoppfatning er det ethvert menneskes rett fritt å kunne velge om man vil gifte seg, og hvem man vil gifte seg med. De fleste av oss tar denne valgfriheten for gitt, men dessverre gjelder ikke dette for alle. Selv i dagens Norge blir unge mennesker tvunget eller presset av sine foreldre til å gifte seg mot sin vilje, dette til tross for at lovverket sier noe annet. Det er oftest barn og unge som utsettes for tvangsekteskap, og eneste vei bort fra et uønsket ekteskap er å bryte med foreldrene og slekten. Et slikt brudd kan være et uoverkommelig skritt å ta for en mindreårig eller ung voksen. Myndighetene Myndighetene har gjennom ulike tiltak forsøkt å ta problemet med tvangsekteskap på alvor. I forbindelse med den nye handlingsplanen mot tvangsekteskap som kom i 2007, uttalte daværende arbeids- og inkluderingsminister Bjarne Håkon Hanssen: Vi kan ikke akseptere at unge jenter og gutter i vårt samfunn blir giftet bort mot sin vilje. Det er viktig at vi hjelper dem på deres premisser. Gjennom satsingen på minoritetsrådgivere og integreringsrådgivere bidrar vi nå til å bygge opp et støtte- og hjelpeapparat for ungdommene dette kan gjelde. Gjennom trygge og gode tiltak skal myndighetene tilby et hjelpeapparat der de som blir utsatt for tvangsekteskap, skal få den støtten de trenger. Hvorfor arrangerte ekteskap? Arrangerte ekteskap har sammenheng med et foreldet kjønnsrollemønster. Det finnes i kulturer der kvinner har en undertrykket stilling og hennes dydighet skal kontrolleres. Mannens ære er også en sentral verdi i denne praksisen. Ved å miste kontrollen over kvinnen føler mannen seg som en hanrei (en bedratt ektemann). På samme måte vil en datter som ikke følger foreldrenes råd, bringe skam over familien, da særlig far, brødre og andre mannlige familiemedlemmer. Tvangsekteskapet er den ytterliggående formen for arrangert ekteskap, og beslutninger tas uten at man rådfører seg med den ene eller begge partene som skal inngå ekteskap. Arrangerte ekteskap – fornuftsekteskap? Det er ikke lenge siden det var vanlig med arrangerte ekteskap også i Norge. Gjennom ekteskap kunne man sikre gode forbindelser mellom slekter. Kulturkollisjon I miljøer der arrangerte ekteskap praktiseres, argumenterer man for stabiliteten i slike ekteskap, i motsetning til den store skilsmissestatistikken for etniske nordmenns kjærlighetsekteskap. Man kan diskutere om det er stabiliteten i familiestrukturene som er det viktigste, eller om den enkeltes frihet og likestilling mellom kjønnene skal telle mer. Et eksempel fra rettsapparatet: \"I 2006 tok Høyesterett for første gang stilling til straffeutmålingen i en straffesak etter den nye bestemmelsen i § 222 andre ledd i straffeloven. En mann ble sammen med en av sine sønner dømt for å ha truet med vold og for å forsøke å tvinge sin eldste datter til å inngå ekteskap da hun var 17 år. I tingretten ble de to dømt til henholdsvis ti og åtte måneders fengsel. Saken ble anket til lagmannsretten, som økte straffen til henholdsvis ett år og ni måneder og ett år og fem måneder. Saken ble videre anket til Høyesterett. Høyesterett understreket at tvangsekteskap er en grov krenkelse av individets frihet og selvstendighet og nesten uten unntak også en grov krenkelse av råderetten over egen kropp. Straffen ble satt til fengsel i to år og seks måneder for faren og to år for sønnen.\" (Handlingsplan for tvangsekteskap, 2007 s. 11)","nn":"Ifølgje den oppfatninga av menneskerettar som vi har i Vesten, er det å fritt kunne velje om ein vil gifte seg, og kven ein vil gifte seg med, ein rett for kvart menneske. Dei fleste av oss reknar denne valfridomen for sjølvsagd, men diverre gjeld det ikkje for alle. Sjølv i Noreg i dag vert unge menneske tvungne eller pressa av foreldra sine til å gifte seg mot sin eigen vilje, trass i at lovverket seier noko anna. Det er oftast barn og unge som vert offer for tvangsekteskap, og den einaste vegen bort frå eit uønskt ekteskap er å bryte med foreldra og slekta. Eit slikt brott kan vere eit uoverkommeleg steg å ta for ein mindreårig eller ung vaksen. Styresmaktene Styresmaktene har gjennom ulike tiltak forsøkt å ta problemet med tvangsekteskap på alvor. I samband med den nye handlingsplanen mot tvangsekteskap som kom i 2007, ytra daverande arbeids- og inkluderingsminister Bjarne Håkon Hanssen: \"Vi kan ikke akseptere at unge jenter og gutter i vårt samfunn blir giftet bort mot sin vilje. Det er viktig at vi hjelper dem på deres premisser. Gjennom satsingen på minoritetsrådgivere og integreringsrådgivere bidrar vi nå til å bygge opp et støtte- og hjelpeapparat for ungdommene dette kan gjelde.\" Gjennom trygge og gode tiltak skal styresmaktene tilby eit hjelpeapparat der dei som opplever tvangsekteskap, skal få den støtta dei treng. Kvifor arrangerte ekteskap? Arrangerte ekteskap har samanheng med eit forelda kjønnsrollemønster. Det finst i kulturar der kvinner har ei undertrykt stilling og dydigheita hennar skal kontrollerast. Æra til mannen er også ein sentral verdi i denne praksisen. Om han mistar kontrollen over kvinna, føler mannen seg som ein hanrei (ein mann som kona har vore utru mot). På same måte vil ei dotter som ikkje følgjer familiens råd, bringe skam over familien, da spesielt far, brør og andre mannlege familiemedlemmer. Tvangsekteskapet er den ytterleggåande forma for arrangert ekteskap, og avgjerder vert fatta utan at ein rådfører seg med den eine av eller begge partane som skal inngå ekteskap. Arrangerte ekteskap – fornuftsekteskap? Det er ikkje lenge sidan det var vanleg med arrangerte ekteskap også i Noreg. Gjennom ekteskap kunne ein sikre gode alliansar mellom slekter. Kulturkollisjon I miljø der arrangerte ekteskap vert praktisert, argumenterer ein for stabiliteten i slike ekteskap, i motsetnad til den store skilsmissestatistikken for kjærleiksekteskapa til etniske nordmenn. Ein kan diskutere om det er stabiliteten i familiestrukturane som er det viktigaste, eller om fridomen til den einskilde og likestillinga mellom kjønna skal telje meir. Eit døme frå rettsapparatet: \"I 2006 tok Høyesterett for første gang stilling til straffeutmålingen i en straffesak etter den nye bestemmelsen i § 222 andre ledd i straffeloven. En mann ble sammen med en av sine sønner dømt for å ha truet med vold og for å forsøke å tvinge sin eldste datter til å inngå ekteskap da hun var 17 år. I tingretten ble de to dømt til henholdsvis ti og åtte måneders fengsel. Saken ble anket til lagmannsretten, som økte straffen til henholdsvis ett år og ni måneder og ett år og fem måneder. Saken ble videre anket til Høyesterett. Høyesterett understreket at tvangsekteskap er en grov krenkelse av individets frihet og selvstendighet og nesten uten unntak også en grov krenkelse av råderetten over egen kropp. Straffen ble satt til fengsel i to år og seks måneder for faren og to år for sønnen.\" (Handlingsplan for tvangsekteskap, 2007, s. 11)"},"highlight":{"content.nn":["Ifølgje den oppfatninga av menneskerettar som vi har i Vesten, er det å fritt kunne velje om ein vil gifte seg, og kven ein vil gifte seg med, ein rett for kvart menneske. Dei fleste av oss reknar denne valfridomen for sjølvsagd, men diverre gjeld det ikkje for alle. Sjølv i Noreg i dag vert unge menneske tvungne eller pressa av foreldra sine til å gifte seg mot sin eigen vilje, trass i at lovverket seier noko anna. Det er oftast barn og unge som vert offer for tvangsekteskap, og den einaste vegen bort frå eit uønskt ekteskap er å bryte med foreldra og slekta. Eit slikt brott kan vere eit uoverkommeleg steg å ta for ein mindreårig eller ung vaksen. Styresmaktene Styresmaktene har gjennom ulike tiltak forsøkt å ta problemet med tvangsekteskap på alvor. I samband med den nye handlingsplanen mot tvangsekteskap som kom i 2007, ytra daverande arbeids- og inkluderingsminister Bjarne Håkon Hanssen: \"Vi kan ikke akseptere at unge jenter og gutter i vårt samfunn blir giftet bort mot sin vilje. Det er viktig at vi hjelper dem på deres premisser. Gjennom satsingen på minoritetsrådgivere og integreringsrådgivere bidrar vi nå til å bygge opp et støtte- og hjelpeapparat for ungdommene dette kan gjelde.\" Gjennom trygge og gode tiltak skal styresmaktene tilby eit hjelpeapparat der dei som opplever tvangsekteskap, skal få den støtta dei treng. Kvifor arrangerte ekteskap? Arrangerte ekteskap har samanheng med eit forelda kjønnsrollemønster. Det finst i kulturar der kvinner har ei undertrykt stilling og dydigheita hennar skal kontrollerast. Æra til mannen er også ein sentral verdi i denne praksisen. Om han mistar kontrollen over kvinna, føler mannen seg som ein hanrei (ein mann som kona har vore utru mot). På same måte vil ei dotter som ikkje følgjer familiens råd, bringe skam over familien, da spesielt far, brør og andre mannlege familiemedlemmer. Tvangsekteskapet er den ytterleggåande forma for arrangert ekteskap, og avgjerder vert fatta utan at ein rådfører seg med den eine av eller begge partane som skal inngå ekteskap. Arrangerte ekteskap – fornuftsekteskap? Det er ikkje lenge sidan det var vanleg med arrangerte ekteskap også i Noreg. Gjennom ekteskap kunne ein sikre gode alliansar mellom slekter. Kulturkollisjon I miljø der arrangerte ekteskap vert praktisert, argumenterer ein for stabiliteten i slike ekteskap, i motsetnad til den store skilsmissestatistikken for kjærleiksekteskapa til etniske nordmenn. Ein kan diskutere om det er stabiliteten i familiestrukturane som er det viktigaste, eller om fridomen til den einskilde og likestillinga mellom kjønna skal telje meir. Eit døme frå rettsapparatet: \"I 2006 tok Høyesterett for første gang stilling til straffeutmålingen i en straffesak etter den nye bestemmelsen i § 222 andre ledd i straffeloven. En mann ble sammen med en av sine sønner dømt for å ha truet med vold og for å forsøke å tvinge sin eldste datter til å inngå ekteskap da hun var 17 år. I tingretten ble de to dømt til henholdsvis ti og åtte måneders fengsel. Saken ble anket til lagmannsretten, som økte straffen til henholdsvis ett år og ni måneder og ett år og fem måneder. Saken ble videre anket til Høyesterett. Høyesterett understreket at tvangsekteskap er en grov krenkelse av individets frihet og selvstendighet og nesten uten unntak også en grov krenkelse av råderetten over egen kropp. Straffen ble satt til fengsel i to år og seks måneder for faren og to år for sønnen.\" (Handlingsplan for tvangsekteskap, 2007, s. 11)"]}}]}},"tags":{"hits":{"total":0,"max_score":null,"hits":[]}}}}]}}"""

    val resultArray = (parse(jsonstring) \ "hits" \ "hits").asInstanceOf[JArray].arr
    val languages = resultArray.map(result => {
      service.getLanguageFromHit(result)
    })

    languages.size should equal(3)
    languages(0).get should equal("nn")
    languages(1).get should equal("nn")
    languages(2).get should equal("nn")

  }

}
