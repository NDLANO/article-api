package no.ndla.articleapi.service.converters

import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.articleapi.integration.LanguageIngress
import no.ndla.validation.EmbedTagRules.ResourceHtmlEmbedTag
import no.ndla.articleapi.model.domain.ImportStatus
import no.ndla.validation.TagAttributes

import scala.util.Success

class HTMLCleanerTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val defaultImportStatus = ImportStatus.empty

  val defaultLanguageIngress = LanguageIngress("Jeg er en ingress", None)
  val defaultLanguageIngressWithHtml = LanguageIngress("<p>Jeg er en ingress</p>", None)

  test("That HTMLCleaner unwraps illegal attributes") {
    val initialContent = TestData.sampleContent.copy(content ="""<body><h1 class="useless">heading<div style="width='0px'">hey</div></h1></body>""")
    val expectedResult = "<section><h1>heading<div>hey</div></h1></section>"
    val Success((result, _)) = htmlCleaner.convert(initialContent, defaultImportStatus)

    result.content should equal(expectedResult)
    result.requiredLibraries.size should equal(0)
  }

  test("That HTMLCleaner unwraps illegal tags") {
    val initialContent = TestData.sampleContent.copy(content ="""<section><h1>heading</h1><henriktag><p>hehe</p></henriktag></section>""")
    val expectedResult = "<section><h1>heading</h1><p>hehe</p></section>"
    val Success((result, _)) = htmlCleaner.convert(initialContent, defaultImportStatus)

    result.content should equal(expectedResult)
    result.requiredLibraries.size should equal(0)
  }

  test("That HTMLCleaner removes comments") {
    val initialContent = TestData.sampleContent.copy(content ="""<section><!-- this is a comment --><h1>heading<!-- comment --></h1></section>""")
    val expectedResult = "<section><h1>heading</h1></section>"
    val Success((result, _)) = htmlCleaner.convert(initialContent, defaultImportStatus)

    result.content should equal(expectedResult)
    result.requiredLibraries.size should equal(0)
  }

  test("That HTMLCleaner removes empty p,div,section,aside tags") {
    val initialContent = TestData.sampleContent.copy(content ="""<h1>not empty</h1><section><p></p><div></div><aside></aside></section>""")
    val expectedResult = "<section><h1>not empty</h1></section>"
    val Success((result, _)) = htmlCleaner.convert(initialContent, defaultImportStatus)

    result.content should equal(expectedResult)
    result.requiredLibraries.size should equal(0)
  }

  test("ingress is extracted when wrapped in <p> tags") {
    val content =
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |<p><strong>Medievanene er i endring.</br></strong></p>
         |</section>
         |<section>
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")

    val expectedContentResult =
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |</section>
         |<section>
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")
    val expectedIngressResult = LanguageIngress("Medievanene er i endring.", None)

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
  }

  test("ingress text is not extracted when not present") {
    val content =
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult =
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")
    val expectedIngressResult = None
    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(expectedIngressResult)
    result.requiredLibraries.size should equal(0)
  }

  test("ingress with word count less than 3 should not be interpreted as an ingress") {
    val content =
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |<p><strong>Medievanener<br></strong></p>
         |</section>
         |<section>
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.content should equal(content)
    result.ingress should equal(None)
  }

  test("ingress image is not extracted when not present") {
    val content =
      """<section>
        |<p><strong>Du har sikkert opplevd rykter og usannheter</strong></p>
        |<ul>
        |<li><a href="#" title="Snopes">Snopes</a></li>
        |</ul>
        |</section>
        |""".stripMargin.replace("\n", "")
    val expectedContentResult = """<section><ul><li><a href="#" title="Snopes">Snopes</a></li></ul></section>"""
    val expectedIngressResult = LanguageIngress("Du har sikkert opplevd rykter og usannheter", None)
    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)
    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
    result.requiredLibraries.size should equal(0)
  }

  test("ingress text is extracted when wrapped in <strong> tags") {
    val content =
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |<strong>Medievanene er i endring.</strong>
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult =
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |<h2>Mediehverdagen</h2></section>""".stripMargin.replace("\n", "")
    val expectedIngressResult = LanguageIngress("Medievanene er i endring.", None)
    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
  }

  test("ingress should not be extracted if not located in the beginning of content") {
    val content =
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |<h2>Mediehverdagen</h2>
         |<p>A paragraph!</p>
         |<p><strong>Medievanene er i endring.</strong><p>
         |</section>""".stripMargin.replace("\n", "")
    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.ingress should be(None)
  }

  test("only the first paragraph should be extracted if strong") {
    val content =
      s"""<section><embed data-align="" data-alt="Hånd som tegner" data-caption="" data-resource="image" data-resource_id="200" data-size="fullbredde"/>
         |<p><strong>Når du skal jobbe med fotoutstilling, er det lurt å sette seg godt inn i tema for utstillingen og bestemme seg for hvilket uttrykk man
         |er ute etter å skape allerede i planleggingsfasen.
         |</strong></p>
         |<h2>Tips til aktuelle verktøy og bruk av verktøy</h2>
         |</section>""".stripMargin
    val expectedContentResult =
      s"""<section><embed data-align="" data-alt="Hånd som tegner" data-caption="" data-resource="image" data-resource_id="200" data-size="fullbredde">
         |<h2>Tips til aktuelle verktøy og bruk av verktøy</h2></section>""".stripMargin.replace("\n", "")
    val expectedIngressResult = LanguageIngress("Når du skal jobbe med fotoutstilling, er det lurt å sette seg godt inn i tema for utstillingen og bestemme seg for hvilket uttrykk man er ute etter å skape allerede i planleggingsfasen.", None)

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
  }

  test("ingress inside a div should be extracted") {
    val nbsp = "\u00a0"
    val content =
      s"""<section><div>
         |<embed data-align="" data-alt="To gutter" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" data-url="http://ndla">
         |<p><strong>$nbsp</strong><strong>Du er et unikt individ, med en rekke personlige egenskaper.</strong></p>
         |</div></section>""".stripMargin
    val expectedContent =
      """<section><div>
        |<embed data-align="" data-alt="To gutter" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" data-url="http://ndla">
        |
        |</div></section>""".stripMargin
    val expectedIngress = Some(LanguageIngress("Du er et unikt individ, med en rekke personlige egenskaper.", None))

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.ingress should be(expectedIngress)
    result.content should be(expectedContent)
  }

  test("ingress inside a nested div should be extracted") {
    val content =
      """<section><div><div>
        |<embed data-align="" data-alt="To gutter" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" data-url="http://ndla">
        |<p><strong>Du er et unikt individ, med en rekke personlige egenskaper.</strong></p>
        |</div><p>do not touch</p></div></section>""".stripMargin
    val expectedContent =
      """<section><div><div>
        |<embed data-align="" data-alt="To gutter" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" data-url="http://ndla">
        |
        |</div><p>do not touch</p></div></section>""".stripMargin
    val expectedIngress = Some(LanguageIngress("Du er et unikt individ, med en rekke personlige egenskaper.", None))

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.ingress should be(expectedIngress)
    result.content should be(expectedContent)
  }

  test("ingres inside first paragraph should be extracted when also div is present") {
    val content =
      """<section><p><strong>correct ingress more than three words</strong></p><div><div>
        |<embed data-align="" data-alt="To gutter" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" data-url="http://ndla">
        |<p><strong>Du er et unikt individ, med en rekke personlige egenskaper.</strong></p>
        |</div><p>do not touch</p></div></section>""".stripMargin
    val expectedContent =
      """<section><div><div>
        |<embed data-align="" data-alt="To gutter" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" data-url="http://ndla">
        |<p><strong>Du er et unikt individ, med en rekke personlige egenskaper.</strong></p>
        |</div><p>do not touch</p></div></section>""".stripMargin
    val expectedIngress = Some(LanguageIngress("correct ingress more than three words", None))

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.ingress should be(expectedIngress)
    result.content should be(expectedContent)
  }

  test("ingress split up into multiple strong elements should be extracted") {
    val content =
      """<section><p><strong>correct ingress more than three words.</strong> <em><strong>look I'm emphasized</strong></em></p><div><div>
        |<embed data-align="" data-alt="To gutter" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" data-url="http://ndla">
        |<p><strong>Du er et unikt individ, med en rekke personlige egenskaper.</strong></p>
        |</div><p>do not touch</p></div></section>""".stripMargin
    val expectedContent =
      """<section><div><div>
        |<embed data-align="" data-alt="To gutter" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" data-url="http://ndla">
        |<p><strong>Du er et unikt individ, med en rekke personlige egenskaper.</strong></p>
        |</div><p>do not touch</p></div></section>""".stripMargin
    val expectedIngress = Some(LanguageIngress("correct ingress more than three words. look I'm emphasized", None))

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.ingress should be(expectedIngress)
    result.content should be(expectedContent)
  }

  test("Ingress should not be fetched if there is content before the nested div that contains it") {
    val content =
      """<section><div class="c-bodybox"><h2>Leksjon 2: Å bli kjend med ein medstudent</h2><p><embed data-align="" data-alt="Studenter snakker sammen. Foto." data-caption="" data-resource="image" data-resource_id="3325" data-size="fullbredde">  </p><h3>Førebuing</h3><p style="padding-left: 30px;"><span style="font-size: x-large;">准备</span></p><p style="padding-left: 30px;">Zhǔnbèi</p><p> </p><p><strong>Dette bør du vite på førehand: </strong></p><table><tbody><tr><td><ul><li>korleis du helsar på nokon på kinesisk</li><li>korleis du spør om og fortel kva du heiter</li></ul></td><td><ul><li>kva slags tiltaleformer, måtar å helse på og namn som brukast i Kina</li></ul></td></tr></tbody></table><p> </p><strong>Dette skal du lære:</strong><table><thead><tr><th>Språk: <ul><li> å seie «God morgon!»</li><li>å spørje om og fortelje om du kjenner nokon eller ikkje</li><li>å seie om nokon er ein venn eller ein medstudent</li><li>å spørje kven nokon er</li><li>å spørje om og fortelje om nokon sin nasjonalitet</li></ul></th><th>Språk, samfunn og kultur:<ul><li>nokre trekk ved kinesarane sitt forhold til utlendingar opp gjennom historia</li><li>korleis det er å vere utlending i Kina</li><li>utlendingar sine haldningar til Kina historisk sett</li></ul></th></tr></thead></table><p> </p><div class="c-bodybox"><strong>Før du startar, tenk igjennom dette:</strong></div><div class="c-bodybox"><strong><table><tbody><tr><td><ul><li>Kva snakkar vi om når vi skal bli kjende med nye klassekameratar i Noreg?</li></ul></td><td><ul><li>Kva haldningar har nordmenn til utlendingar, og korleis ser utlendingar på Noreg?</li><li>Kva haldningar har vi til Kina og kinesarane?</li></ul></td></tr></tbody></table></strong><strong><p> </p></strong></div></div> </section>"""
    val expectedContent = """<section><div class="c-bodybox"><h2>Leksjon 2: Å bli kjend med ein medstudent</h2><embed data-align="" data-alt="Studenter snakker sammen. Foto." data-caption="" data-resource="image" data-resource_id="3325" data-size="fullbredde"><h3>Førebuing</h3><p>准备</p><p>Zhǔnbèi</p><p><strong>Dette bør du vite på førehand: </strong></p><table><tbody><tr><td><ul><li>korleis du helsar på nokon på kinesisk</li><li>korleis du spør om og fortel kva du heiter</li></ul></td><td><ul><li>kva slags tiltaleformer, måtar å helse på og namn som brukast i Kina</li></ul></td></tr></tbody></table><strong>Dette skal du lære:</strong><table><thead><tr><th>Språk: <ul><li> å seie «God morgon!»</li><li>å spørje om og fortelje om du kjenner nokon eller ikkje</li><li>å seie om nokon er ein venn eller ein medstudent</li><li>å spørje kven nokon er</li><li>å spørje om og fortelje om nokon sin nasjonalitet</li></ul></th><th>Språk, samfunn og kultur:<ul><li>nokre trekk ved kinesarane sitt forhold til utlendingar opp gjennom historia</li><li>korleis det er å vere utlending i Kina</li><li>utlendingar sine haldningar til Kina historisk sett</li></ul></th></tr></thead></table><div class="c-bodybox"><strong>Før du startar, tenk igjennom dette:</strong></div><div class="c-bodybox"><strong><table><tbody><tr><td><ul><li>Kva snakkar vi om når vi skal bli kjende med nye klassekameratar i Noreg?</li></ul></td><td><ul><li>Kva haldningar har nordmenn til utlendingar, og korleis ser utlendingar på Noreg?</li><li>Kva haldningar har vi til Kina og kinesarane?</li></ul></td></tr></tbody></table></strong></div></div></section>"""
    val expectedIngress = None

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.ingress should equal(expectedIngress)
    result.content should equal(expectedContent)
  }

  test("standalone text in a section is wrapped in <p> tags") {
    val content =
      s"""<section>
         |Medievanene er i endring.
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult =
      s"""<section>
         |<p>Medievanene er i endring.</p>
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.content should equal(expectedContentResult)
  }

  test("spans with lang attribute is kept as <span> tags") {
    val content =
      s"""<section>
          |<span xml:lang="nb" lang="nb">HyperText Markup Language</span>
          |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult=
      s"""<section>
          |<span lang="nb">HyperText Markup Language</span>
          |</section>""".stripMargin.replace("\n", "")

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.content should equal(expectedContentResult)
  }

  test("spans with xml:lang attribute is kept as <span> tags and lang tag is inserted") {
    val content =
      s"""<section>
          |<span xml:lang="nb">HyperText Markup Language</span>
          |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult=
      s"""<section>
          |<span lang="nb">HyperText Markup Language</span>
          |</section>""".stripMargin.replace("\n", "")

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.content should equal(expectedContentResult)
  }

  test("spans with with no attributes is unwrapped") {
    val content =
      s"""<section>
          |<span>HyperText Markup Language</span>
          |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult=
      s"""<section>
          |<p>HyperText Markup Language</p>
          |</section>""".stripMargin.replace("\n", "")

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.content should equal(expectedContentResult)
  }

  test("standalone text and <em> tags in a section should be wrapped in <p> tags") {
    val origContent =
      """<section>
        |<h2>De kursiverte ordene og ordforbindelsene i denne teksten er enten skrevet feil eller brukt feil. Hva er riktig?</h2>
        |Det har <em>skjelden</em> vært så mange <em>tilstede</em> som <em>igår</em>.
        |<h1>lolol</h1>
        |should be wrapped
        |</section>""".stripMargin.replace("\n", "")

    val expectedContent =
      """<section>
        |<h2>De kursiverte ordene og ordforbindelsene i denne teksten er enten skrevet feil eller brukt feil. Hva er riktig?</h2>
        |<p>
        |Det har <em>skjelden</em> vært så mange <em>tilstede</em> som <em>igår</em>.
        |</p>
        |<h1>lolol</h1>
        |<p>should be wrapped</p>
        |</section>""".stripMargin.replace("\n", "")

    val Success((res, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = origContent), defaultImportStatus)
    res.content should equal(expectedContent)
  }

  test("That HTMLCleaner do not insert ingress if already added from seperate table") {
    val content =
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |<p><strong>Medievanene er i endring.</strong></p>
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult =
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |<p><strong>Medievanene er i endring.</strong></p>
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")

    val notExpectedIngressResult = LanguageIngress("Medievanene er i endring.", None)
    val expectedIngressResult = LanguageIngress("Jeg er en ingress", None)

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content, ingress = Some(defaultLanguageIngress)), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
    result.ingress should not equal Some(notExpectedIngressResult)

  }

  test("That HTMLCleaner removes all tags in ingress from separate table") {
    val content =
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |<p><strong>Medievanene er i endring.</strong></p>
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult =
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |<p><strong>Medievanene er i endring.</strong></p>
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")

    val expectedIngressResult = LanguageIngress("Jeg er en ingress", None)

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content, ingress = Some(defaultLanguageIngressWithHtml)), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
  }

  test("HTMLCleaner extracts two first string paragraphs as ingress") {
    val content =
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |<p><strong>Medievanene er i endring.</strong></p>
         |<p><strong>Er egentlig medievanene i endring</strong></p>
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult =
      s"""<section>
         |<$ResourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")

    val expectedIngressResult = LanguageIngress("Medievanene er i endring. Er egentlig medievanene i endring", None)

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = content), defaultImportStatus)

    result.ingress should equal(Some(expectedIngressResult))
    result.content should equal(expectedContentResult)
  }

  test("elements are replaced with data-caption text in meta description") {
    val content = TestData.sampleContent.copy(content = "", metaDescription =s"""Look at this image <$ResourceHtmlEmbedTag data-resource="image" data-caption="image caption">""")
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)

    result.metaDescription should equal("Look at this image image caption")
  }

  test("HTML characters are escaped in meta description") {
    val content = TestData.sampleContent.copy(content = "", metaDescription ="""Hei dette er et mindre enn tegn <> nice""")
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)

    result.metaDescription should equal("Hei dette er et mindre enn tegn &lt;&gt; nice")
  }

  test("an embed-image as the first element inside p tags are moved out of p tag") {
    val image1 = s"""<$ResourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img.jpg">"""
    val content = TestData.sampleContent.copy(content =s"""<section><p>${image1}sample text</p></section>""")

    val expectedResult = s"""<section>$image1<p>sample text</p></section>"""
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)
    result.content should equal(expectedResult)
  }

  test("an embed-image inside p tags are moved out of p tag") {
    val image1 = s"""<$ResourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img.jpg">"""
    val content = TestData.sampleContent.copy(content =s"""<section><p><br>${image1}sample text</p></section>""")

    val expectedResult = s"""<section>$image1<p><br>sample text</p></section>"""
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)
    result.content should equal(expectedResult)
  }

  test("an embed-image inside p tags with br are moved out of p tag and p tag is removed") {
    val image1 = s"""<$ResourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img.jpg">"""
    val content = TestData.sampleContent.copy(content =s"""<section><p><br />$image1</p></section>""")

    val expectedResult = s"""<section>$image1</section>"""
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)
    result.content should equal(expectedResult)
  }

  test("embed-images inside p tags are moved out of p tag and p is removed if empty") {
    val image1 = s"""<$ResourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img1.jpg">"""
    val image2 = s"""<$ResourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img2.jpg">"""
    val image3 = s"""<$ResourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img3.jpg">"""
    val content = TestData.sampleContent.copy(content =s"""<section><p>$image1$image2 $image3</p></section>""")

    val expectedResult = s"""<section>$image1$image2$image3</section>"""
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)
    result.content should equal(expectedResult)
  }

  test("embed-images inside nested p tags are moved out of p tag and p is removed if empty") {
    val image = s"""<$ResourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img1.jpg">"""
    val content = TestData.sampleContent.copy(content =s"""<section><p><strong>$image</strong></p></section>""")

    val expectedResult = s"""<section>$image</section>"""
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)
    result.content should equal(expectedResult)
  }

  test("p tags without images are left untouched") {
    val originalContent = """<section><p>sample text</p></section>"""
    val content = TestData.sampleContent.copy(content = originalContent)
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)

    result.content should equal(originalContent)
  }

  test("moveMisplacedAsideTags should move aside tags located at the start of the article further down") {
    val image = s"""<$ResourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img1.jpg">"""
    val paragraph = "<p>sample text</p>"
    val aside = "<aside>This block should not be on top</aside>"

    val originalContent = s"""<section>$aside$image$paragraph</section>"""
    val expectedContent = s"""<section>$image$aside$paragraph</section>"""
    val Success((result1, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)
    result1.content should equal(expectedContent)

    val originalContent2 = s"""<section>$aside$image</section>"""
    val expectedContent2 = s"""<section>$image$aside</section>"""
    val Success((result2, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent2), defaultImportStatus)
    result2.content should equal(expectedContent2)

    val originalContent3 = s"""<section>$aside</section>"""
    val expectedContent3 = s"""<section>$aside</section>"""
    val Success((result3, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent3), defaultImportStatus)
    result3.content should equal(expectedContent3)
  }

  test("all content must be wrapped in sections") {
    val original = "<h1>hello</h1><p>content</p>"
    val content = TestData.sampleContent.copy(content = original)
    val expected = s"<section>$original</section>"

    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)
    result.content should equal(expected)
  }

  test("elements not inside a section should be moved to previous section") {
    val original =
      "<section>" +
        "<h1>hello</h1>" +
        "</section>" +
        "<p>here, have a content</p>" +
        "<p>you deserve it</p>" +
        "<section>" +
        "<p>outro</p>" +
        "</section>"
    val content = TestData.sampleContent.copy(content = original)
    val expected =
      "<section>" +
        "<h1>hello</h1>" +
        "<p>here, have a content</p>" +
        "<p>you deserve it</p>" +
        "</section>" +
        "<section>" +
        "<p>outro</p>" +
        "</section>"

    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)

    result.content should equal(expected)
  }

  test("an empty section should be inserted as the first element if first element is not a section") {
    val original =
      "<h1>hello</h1>" +
        "<section>" +
        "<p>here, have a content</p>" +
        "</section>" +
        "<p>you deserve it</p>" +
        "<section>" +
        "<p>outro</p>" +
        "</section>"
    val content = TestData.sampleContent.copy(content = original)
    val expected =
      "<section>" +
        "<h1>hello</h1>" +
        "</section>" +
        "<section>" +
        "<p>here, have a content</p>" +
        "<p>you deserve it</p>" +
        "</section>" +
        "<section>" +
        "<p>outro</p>" +
        "</section>"


    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)

    result.content should equal(expected)
  }

  test("an empty document should not be wrapped in a section") {
    val original = ""
    val content = TestData.sampleContent.copy(content = original)
    val expected = ""

    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)
    result.content should equal(expected)
  }

  test("first section with only image should be merged with the second section") {
    val originalContent =
      """<section><embed data-resource="image" /></section>""" +
        "<section><aside><h2>Tallene</h2></aside><p><strong>Dette er en oversikt over tall</strong></p></section>"
    val expectedContent = """<section><embed data-resource="image"><aside><h2>Tallene</h2></aside></section>"""
    val expectedIngress = "Dette er en oversikt over tall"

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)

    result.content should equal(expectedContent)
    result.ingress should equal(Some(LanguageIngress(expectedIngress, None)))
  }

  test("lists with alphanumeric bullets points should be properly converted") {
    val originalContent =
      """<section>
        |<ol style="list-style-type: lower-alpha;">
        |<li>Definer makt</li>
        |</ol>
        |</section>""".stripMargin.replace("\n", "")
    val expectedContent =
      s"""<section>
         |<ol ${TagAttributes.DataType}="letters">
         |<li>Definer makt</li>
         |</ol>
         |</section>""".stripMargin.replace("\n", "")

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)

    result.content should equal(expectedContent)
  }

  test("Ingress should not be merged if image is placed in ingress section") {
    val originalContent =
      """<section><div><p><embed data-align="" data-alt="Identitet (kollasje)" data-caption="" data-resource="image" data-resource_id="1" data-size="fullbredde"><strong></strong></p><p><strong>Du er et unikt individ!<br></strong></p></div></section><section><div><p>Er det de genene vi arver fra foreldrene og familien vår, eller er det forholdene vi vokser opp under, for eksempel hjemstedet, familien, venner og skolen? Det er summen av arv og miljø som danner identiteten vår.</p><h2>Normer og regler</h2><p>Normer og regler former oss som individer.</p></div></section>"""

    val expectedContent = """<section><embed data-align="" data-alt="Identitet (kollasje)" data-caption="" data-resource="image" data-resource_id="1" data-size="fullbredde"><div><p>Er det de genene vi arver fra foreldrene og familien vår, eller er det forholdene vi vokser opp under, for eksempel hjemstedet, familien, venner og skolen? Det er summen av arv og miljø som danner identiteten vår.</p><h2>Normer og regler</h2><p>Normer og regler former oss som individer.</p></div></section>"""
    val expectedIngress = """Du er et unikt individ!"""


    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)

    result.content should equal(expectedContent)
    result.ingress should equal(Some(LanguageIngress(expectedIngress, None)))
  }

  test("Ingress should not be merged if brightcove placed in ingress section") {
    val originalContent =
      """<section>
        |        <div><p><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:86043"><strong></strong></p><p><strong>Ulike kulturer har ulike måter å organisere samfunnet sitt på, de har forskjellige samfunnskontrakter.</strong></p></div></section><section>
        |        <div class="c-bodybox"><p>Den norske samfunnskontrakten oppmuntrer til at vi skal være aktive i samfunnslivet og ta stilling i politiske spørsmål.</p><p> </p><p>– Nordmenn med flerkulturell bakgrunn glimrer ofte med sitt fravær i interesseorganisasjoner og politiske partier.</p></div></section>""".stripMargin

    val expectedContent = """<section><embed data-account="4806596774001" data-caption="" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:86043"><div class="c-bodybox"><p>Den norske samfunnskontrakten oppmuntrer til at vi skal være aktive i samfunnslivet og ta stilling i politiske spørsmål.</p><p>– Nordmenn med flerkulturell bakgrunn glimrer ofte med sitt fravær i interesseorganisasjoner og politiske partier.</p></div></section>"""
    val expectedIngress = """Ulike kulturer har ulike måter å organisere samfunnet sitt på, de har forskjellige samfunnskontrakter."""

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)

    result.content should equal(expectedContent)
    result.ingress should equal(Some(LanguageIngress(expectedIngress, None)))
  }

  test("Merging should not happen in cases where there are a container with more info than an image") {
    val originalContent =
      """<section>
        |        <div class="c-bodybox"><p><embed data-align="" data-alt="Journalist Mads A. Andersen foran PC-skjerm i VGs redaksjonslokale. Fotografi." data-caption="" data-resource="image" data-resource_id="6" data-size="fullbredde">  </p><p><strong>Det er en stille dag i redaksjonen.</strong></p><p> </p><h2>Oppdrag</h2><ol><li>Du skal lage en nyhetssak om bankranet til nyhetssendingen på lokalradio kl. 17.30 og til den lokale TV-sendingen kl. 18.40 samme dag.</li><li>Skriv teksten til den nyhetsmeldingen du vil ha på radio.</li></ol></div> </section><section>
        |        <h2>Kildeliste</h2> <p><strong>Kilde 1: Bankansatt</strong></p> <p><embed data-account="4806596774001" data-caption="Kildeeksempel 1: Banksjef" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:78120"> </p> </section>""".stripMargin

    val expectedContent = """<section><div class="c-bodybox"><embed data-align="" data-alt="Journalist Mads A. Andersen foran PC-skjerm i VGs redaksjonslokale. Fotografi." data-caption="" data-resource="image" data-resource_id="6" data-size="fullbredde"><h2>Oppdrag</h2><ol><li>Du skal lage en nyhetssak om bankranet til nyhetssendingen på lokalradio kl. 17.30 og til den lokale TV-sendingen kl. 18.40 samme dag.</li><li>Skriv teksten til den nyhetsmeldingen du vil ha på radio.</li></ol></div></section><section><h2>Kildeliste</h2><p><strong>Kilde 1: Bankansatt</strong></p><embed data-account="4806596774001" data-caption="Kildeeksempel 1: Banksjef" data-player="BkLm8fT" data-resource="brightcove" data-videoid="ref:78120"></section>"""

    val expectedIngress = """Det er en stille dag i redaksjonen."""

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)
    result.content should equal(expectedContent)
    result.ingress should equal(Some(LanguageIngress(expectedIngress, None)))
  }

  test("Ingress should still be extracted if first <p> is only an embed") {
    val originalContent = """<section><p><embed data-height="337px" data-resource="ndla-filmiundervisning" data-url="//ndla.filmiundervisning.no/film/ndlafilm.aspx?filmId=12414" data-width="632px"></p><p><strong>I en film er det bildenes rekkefølge som skaper sammenheng og mening.</strong></p><p>Når vi ser to bilder.</p></section><div class="paragraph"><div class="full"><p><embed data-height="337px" data-resource="ndla-filmiundervisning" data-url="//ndla.filmiundervisning.no/film/ndlafilm.aspx?filmId=12414" data-width="632px">&#xa0;</p><p><strong>Something</strong></p></div></div>"""
    val expectedContent = """<section><p><embed data-height="337px" data-resource="ndla-filmiundervisning" data-url="//ndla.filmiundervisning.no/film/ndlafilm.aspx?filmId=12414" data-width="632px"></p><p>Når vi ser to bilder.</p><div class="paragraph"><div class="full"><p><embed data-height="337px" data-resource="ndla-filmiundervisning" data-url="//ndla.filmiundervisning.no/film/ndlafilm.aspx?filmId=12414" data-width="632px"> </p><p><strong>Something</strong></p></div></div></section>"""

    val expectedIngress = """I en film er det bildenes rekkefølge som skaper sammenheng og mening."""

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)
    result.content should equal(expectedContent)
    result.ingress should equal(Some(LanguageIngress(expectedIngress, None)))
  }

  test("Divs with no siblings in asides should be unwrapped") {
    val originalContent =
      """<section><embed data-resource="image" /></section>""" +
        "<section><aside><div><h2>Tallene</h2></div></aside><p><strong>Dette er en oversikt over tall</strong></p></section>"
    val expectedContent = """<section><embed data-resource="image"><aside><h2>Tallene</h2></aside></section>"""

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)

    result.content should equal(expectedContent)
  }

  test("Nested divs with no siblings in asides should be unwrapped") {
    val originalContent =
      """<section><embed data-resource="image" /></section>""" +
        "<section><aside><div><div><div><h2>Tallene</h2></div></div></div></aside><p><strong>Dette er en oversikt over tall</strong></p></section>"
    val expectedContent = """<section><embed data-resource="image"><aside><h2>Tallene</h2></aside></section>"""

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)

    result.content should equal(expectedContent)
  }

  test("Divs in asides should not be unwrapped if there are siblings") {
    val originalContent =
      """<section><embed data-resource="image" /></section>""" +
        "<section><aside><div>yolo</div><div>lolol</div></aside><p><strong>Dette er en oversikt over tall</strong></p></section>"
    val expectedContent = """<section><embed data-resource="image"><aside><div>yolo</div><div>lolol</div></aside></section>"""

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)

    result.content should equal(expectedContent)
  }

  test("details should be moved out of div boxes") {
    val originalContent = """<section><div><details><summary>nice</summary>nice</details></div></section>"""
    val expectedContent = """<section><details><summary>nice</summary>nice</details></section>"""

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)
    result.content should equal(expectedContent)
  }

  test("details should be moved out of nested div boxes") {
    val originalContent = """<section><div><div class="yolo"><div><div><details><summary>nice</summary>nice</details></div></div></div></div></section>"""
    val expectedContent = """<section><details><summary>nice</summary>nice</details></section>"""

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)
    result.content should equal(expectedContent)
  }

  test("details should not be moved out of div boxes if it has siblings") {
    val originalContent = """<section><div><div><details><summary>nice</summary>nice</details></div><p>here is a paragraph</p></div></section>"""
    val expectedContent = """<section><div><details><summary>nice</summary>nice</details><p>here is a paragraph</p></div></section>"""

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)

    result.content should equal(expectedContent)
  }

  test("H3s should be converted to H2s if no H2s are used") {
    val originalContent = """<section><h1>hi</h1><h3>sup</h3><div>nice<h3>doge</h3></div></section>"""
    val expectedContent = """<section><h1>hi</h1><h2>sup</h2><div>nice<h2>doge</h2></div></section>"""

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)
    result.content should equal(expectedContent)

    val originalContent2 = """<section><h2>hi</h2><h3>sup</h3></section>"""
    val Success((result2, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent2), defaultImportStatus)
    result2.content should equal(originalContent2)
  }

  test("Strongs should be moved into p tags") {
    val originalContent = """<section><p>Hey</p><strong>Mister</strong></section>"""
    val expectedContent = """<section><p>Hey</p><p><strong>Mister</strong></p></section>"""

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content = originalContent), defaultImportStatus)
    result.content should equal(expectedContent)
  }

}
