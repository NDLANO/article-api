package no.ndla.articleapi.service.converters

import no.ndla.articleapi.UnitSuite
import no.ndla.articleapi.integration.{LanguageContent, LanguageIngress}
import no.ndla.articleapi.model.ImportStatus

class HTMLCleanerTest extends UnitSuite {
  val nodeId = "1234"
  val defaultLanguageContent = LanguageContent(nodeId, nodeId, """<article><!-- this is a comment --><h1>heading<!-- comment --></h1></article>""", Some("en"))
  val defaultImportStatus = ImportStatus(Seq(), Seq())

  test("That HTMLCleaner unwraps illegal attributes") {
    val initialContent = LanguageContent(nodeId, nodeId, """<body><article><h1 class="useless">heading<div style="width='0px'">hey</div></h1></article></body>""", Some("en"))
    val expectedResult = "<article><h1>heading<div>hey</div></h1></article>"
    val (result, status) = HTMLCleaner.convert(initialContent, defaultImportStatus)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That HTMLCleaner unwraps illegal tags") {
    val initialContent = LanguageContent(nodeId, nodeId, """<article><h1>heading</h1><henriktag>hehe</henriktag></article>""", Some("en"))
    val expectedResult = "<article><h1>heading</h1>hehe</article>"
    val (result, status) = HTMLCleaner.convert(initialContent, defaultImportStatus)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That HTMLCleaner removes comments") {
    val initialContent = LanguageContent(nodeId, nodeId, """<article><!-- this is a comment --><h1>heading<!-- comment --></h1></article>""", Some("en"))
    val expectedResult = "<article><h1>heading</h1></article>"
    val (result, status) = HTMLCleaner.convert(initialContent, defaultImportStatus)

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("ingress is extracted when wrapped in <p> tags") {
    val content = """<section>
                   |<figure data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-id="1" data-resource="image" data-alt="Mobiltelefon sender SMS"></figure>
                   |<p><strong>Medievanene er i endring.</br></strong></p>
                   |</section>
                   |<section>
                   |<h2>Mediehverdagen</h2>
                 |</section>""".stripMargin.replace("\n", "")
    val content2 = """<section> <figure data-size="fullbredde" data-url="http://image-api/images/5359" data-align="" data-id="9" data-resource="image" data-alt="To personer på hver sin dataarbeidplass. Ei kvinne som står og en mann som sitter. Tegning." data-caption="Redigeringsarbeid kan kreve mye tid ved datamaskinen. Hun lar kroppen få avveksling og litt aktivisering ved å stå en stund, han passer arm og skulder ved å bruke annet pekeverktøy enn vanlig datamus."></figure><p><strong>Når man driver med medieproduksjon, er det mye arbeid som må gjøres på dataarbeidsplassen. Det kan bli timer med skriving på tastatur, og enda flere timer med redigering og bruk av datamus. Da er det bra å kunne skifte arbeidsstilling og bruke ulike pekeverktøy. <br /></strong></p></section><section> <p>Det som kan gi helse- og sikkerhetsproblemer på en dataarbeidsplass, er:</p><ul><li>stillesitting</li><li>sittestilling</li><li>ensformig bevegelsesmønster</li><li>strømførende kabler, kontakter og elektronisk utstyr</li><li>skjermtitting og feil belysning</li><li>mangelfullt reinhold</li><li>generelt rot</li></ul><p>Om hvordan du skal sette opp dataarbeidsplassen og motarbeide og lindre vanlige plager, kan du lese mange steder. For eksempel i </p><figure data-resource="content-link" data-id="8" data-content-id="336" data-link-text="ergonomi"></figure>-kapittelet for kroppsøving Vg2 her på NDLA, eller på<a href="http://ergonomiportalen.no/" title=" Ergonomiportalen"> Ergonomiportalen</a>.<p>For deg som kommer til å jobbe med medieproduksjon, kan det være spesielt viktig å tenke på å variere verktøybruk og arbeidsstilling, og hvordan du sørger for å ta vare på det elektroniske utstyret slik at det ikke gir fare for brann og støtskader.</p><h2>Bruk forskjellig pekeverktøy</h2><p>Når du jobber med medieproduksjon, kan du måtte bruke pekeverktøyet mye mer enn en som mest sitter og skriver. Det gir stor belastning på arm, skuldre og nakke når disse bevegelsene blir for ensformige. Det er derfor lurt å veksle mellom ulike slags pekeverktøy. Det går fint å ha flere typer koblet til maskinen samtidig.</p><table><tbody><tr><td><figure data-size="liten" data-url="http://api.test.ndla.no/images/5358" data-align="" data-id="6" data-resource="image" data-alt="Datamus med styrekule for tommelen. Fotografi." data-caption=""></figure></td><td><p>Datamus med styrekule gjør at du ikke trenger å bevege armen. I stedet bruker du tommelen til å rulle med, mens musa ligger stille. Den avlaster overarm, skulder og nakke og er også veldig grei når du har liten plass eller dårlig underlag for vanlig mus.</p></td></tr><tr><td><figure data-size="liten" data-url="http://api.test.ndla.no/images/5357" data-align="" data-id="5" data-resource="image" data-alt="Vertikal datamus. Foto." data-caption=""></figure></td><td><p>Med en joystick-mus eller en vertikal mus unngår du å vri underarmen i forhold til albuen. Da belaster du senene i håndledd og underarm mindre,</p></td></tr><tr><td><figure data-size="liten" data-url="http://api.test.ndla.no/images/5356" data-align="" data-id="4" data-resource="image" data-alt="Tegnebrett med penn. Foto." data-caption=""></figure></td><td><p>Tegnebrettet kan du også bruke som pekeverktøy. Du må bevege håndleddet og flytte hånden noe, men bevegelsesmønsteret blir annerledes enn for vanlig datamus.</p></td></tr></tbody></table><h2>Skift arbeidsstilling og reis deg opp</h2><p>Langvarig stillesitting er ikke godt for kropp og blodomløp. Når du har arbeidsøkter som kan gi sammenhengende timer ved datamaskinen, bør du derfor passe på å å få reist deg opp og beveget deg innimellom. Har du mulighet til det, er det lurt å ha et databord som kan heises og senkes. Da kan du veksle mellom å stå og å sitte mens du arbeider. Slike bord finnes både med elektrisk heisefunkjson og med sveiv.</p><figure data-resource="external" data-id="3" data-url="http://www.vg.no/spesial/2014/stillesitting/"></figure><h2>Hold hender og berøringsflater reine</h2><p>Kamera, tastatur, tegnebrett, datamus, mobiltelefon, nettbrett, miksebrett – det er mye man fingrer med gjennom en arbeidsdag som medieprodusent. For å unngå at virus og bakterier smitter og sprer seg, bør du derfor ha reine hender når du håndterer utstyret, og ofte gjøre reint flater du er borti med hender og fingre.</p><figure data-resource="h5p" data-id="2" data-url="http://ndla.no/h5p/embed/26136"></figure><h2>Pass på strøm- og datakablene</h2><p>Kabler og kontakter leder strøm og kan føre til brann om de blir for varme eller ikke er i orden. I tillegg kan mye kabelrot samle støv og gjøre reingjøring vanskelig, og i verste fall fungere som snubletråder.</p><ul><li>Vær helst til stede når du lader utstyr.</li><li>Ladere bør tas ut av stikkontaktene når de ikke brukes.</li><li>Pass på at ladere, kontakter og bærbare enheter ikke er dekket til når de er koblet til strøm, at de får luft, og at de ikke ligger på noe som lett tar fyr.</li><li>Sjekk regelmessig at isolasjonen på kablene er hel. Erstatt med nye kabler dersom den er sprukket.</li><li>Bruk minimalt med skjøteledninger og padder og aldri skjøteledning i skjøteledning.</li><li>Hold orden på ledningene. Noen kan snuble i dem, eller ledningene kan bli skadet fordi de for eksempel blir klemt eller bøyd.</li></ul><figure data-size="fullbredde" data-url="http://api.test.ndla.no/images/5355" data-align="" data-id="1" data-resource="image" data-alt="Oppkveilet ladekabel for iPhone som er full av små hakk og perforeringer. Foto." data-caption="Isolasjonen på denne ladekabelen er ødelagt. Da bør den ikke brukes mer, selv om den virker."></figure></section>"""

    val expectedContentResult = """<section><h2>Mediehverdagen</h2></section>"""
    val expectedContentResult2 = """<section> <p>Det som kan gi helse- og sikkerhetsproblemer på en dataarbeidsplass, er:</p><ul><li>stillesitting</li><li>sittestilling</li><li>ensformig bevegelsesmønster</li><li>strømførende kabler, kontakter og elektronisk utstyr</li><li>skjermtitting og feil belysning</li><li>mangelfullt reinhold</li><li>generelt rot</li></ul><p>Om hvordan du skal sette opp dataarbeidsplassen og motarbeide og lindre vanlige plager, kan du lese mange steder. For eksempel i </p><figure data-resource="content-link" data-id="8" data-content-id="336" data-link-text="ergonomi"></figure>-kapittelet for kroppsøving Vg2 her på NDLA, eller på<a href="http://ergonomiportalen.no/" title=" Ergonomiportalen"> Ergonomiportalen</a>.<p>For deg som kommer til å jobbe med medieproduksjon, kan det være spesielt viktig å tenke på å variere verktøybruk og arbeidsstilling, og hvordan du sørger for å ta vare på det elektroniske utstyret slik at det ikke gir fare for brann og støtskader.</p><h2>Bruk forskjellig pekeverktøy</h2><p>Når du jobber med medieproduksjon, kan du måtte bruke pekeverktøyet mye mer enn en som mest sitter og skriver. Det gir stor belastning på arm, skuldre og nakke når disse bevegelsene blir for ensformige. Det er derfor lurt å veksle mellom ulike slags pekeverktøy. Det går fint å ha flere typer koblet til maskinen samtidig.</p><table><tbody><tr><td><figure data-size="liten" data-url="http://api.test.ndla.no/images/5358" data-align="" data-id="6" data-resource="image" data-alt="Datamus med styrekule for tommelen. Fotografi." data-caption=""></figure></td><td><p>Datamus med styrekule gjør at du ikke trenger å bevege armen. I stedet bruker du tommelen til å rulle med, mens musa ligger stille. Den avlaster overarm, skulder og nakke og er også veldig grei når du har liten plass eller dårlig underlag for vanlig mus.</p></td></tr><tr><td><figure data-size="liten" data-url="http://api.test.ndla.no/images/5357" data-align="" data-id="5" data-resource="image" data-alt="Vertikal datamus. Foto." data-caption=""></figure></td><td><p>Med en joystick-mus eller en vertikal mus unngår du å vri underarmen i forhold til albuen. Da belaster du senene i håndledd og underarm mindre,</p></td></tr><tr><td><figure data-size="liten" data-url="http://api.test.ndla.no/images/5356" data-align="" data-id="4" data-resource="image" data-alt="Tegnebrett med penn. Foto." data-caption=""></figure></td><td><p>Tegnebrettet kan du også bruke som pekeverktøy. Du må bevege håndleddet og flytte hånden noe, men bevegelsesmønsteret blir annerledes enn for vanlig datamus.</p></td></tr></tbody></table><h2>Skift arbeidsstilling og reis deg opp</h2><p>Langvarig stillesitting er ikke godt for kropp og blodomløp. Når du har arbeidsøkter som kan gi sammenhengende timer ved datamaskinen, bør du derfor passe på å å få reist deg opp og beveget deg innimellom. Har du mulighet til det, er det lurt å ha et databord som kan heises og senkes. Da kan du veksle mellom å stå og å sitte mens du arbeider. Slike bord finnes både med elektrisk heisefunkjson og med sveiv.</p><figure data-resource="external" data-id="3" data-url="http://www.vg.no/spesial/2014/stillesitting/"></figure><h2>Hold hender og berøringsflater reine</h2><p>Kamera, tastatur, tegnebrett, datamus, mobiltelefon, nettbrett, miksebrett – det er mye man fingrer med gjennom en arbeidsdag som medieprodusent. For å unngå at virus og bakterier smitter og sprer seg, bør du derfor ha reine hender når du håndterer utstyret, og ofte gjøre reint flater du er borti med hender og fingre.</p><figure data-resource="h5p" data-id="2" data-url="http://ndla.no/h5p/embed/26136"></figure><h2>Pass på strøm- og datakablene</h2><p>Kabler og kontakter leder strøm og kan føre til brann om de blir for varme eller ikke er i orden. I tillegg kan mye kabelrot samle støv og gjøre reingjøring vanskelig, og i verste fall fungere som snubletråder.</p><ul><li>Vær helst til stede når du lader utstyr.</li><li>Ladere bør tas ut av stikkontaktene når de ikke brukes.</li><li>Pass på at ladere, kontakter og bærbare enheter ikke er dekket til når de er koblet til strøm, at de får luft, og at de ikke ligger på noe som lett tar fyr.</li><li>Sjekk regelmessig at isolasjonen på kablene er hel. Erstatt med nye kabler dersom den er sprukket.</li><li>Bruk minimalt med skjøteledninger og padder og aldri skjøteledning i skjøteledning.</li><li>Hold orden på ledningene. Noen kan snuble i dem, eller ledningene kan bli skadet fordi de for eksempel blir klemt eller bøyd.</li></ul><figure data-size="fullbredde" data-url="http://api.test.ndla.no/images/5355" data-align="" data-id="1" data-resource="image" data-alt="Oppkveilet ladekabel for iPhone som er full av små hakk og perforeringer. Foto." data-caption="Isolasjonen på denne ladekabelen er ødelagt. Da bør den ikke brukes mer, selv om den virker."></figure></section>"""

    val expectedIngressResult = LanguageIngress(Some("Medievanene er i endring."), Some("http://image-api/images/5452"))
    val expectedIngressResult2 = LanguageIngress(Some("Når man driver med medieproduksjon, er det mye arbeid som må gjøres på dataarbeidsplassen. Det kan bli timer med skriving på tastatur, og enda flere timer med redigering og bruk av datamus. Da er det bra å kunne skifte arbeidsstilling og bruke ulike pekeverktøy."), Some("http://image-api/images/5359"))

    val (result, status) = HTMLCleaner.convert(defaultLanguageContent.copy(content=content2), defaultImportStatus)
    result.content should equal(expectedContentResult2)
    result.ingress should equal(Some(expectedIngressResult2))
  }

  test("ingress text is not extracted when not present") {
    val content = """<section>
                    |<figure data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-id="1" data-resource="image" data-alt="Mobiltelefon sender SMS"></figure>
                    |<h2>Mediehverdagen</h2>
                    |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult = """<section><h2>Mediehverdagen</h2></section>"""
    val expectedIngressResult = LanguageIngress(None, Some("http://image-api/images/5452"))
    val (result, status) = HTMLCleaner.convert(defaultLanguageContent.copy(content=content), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
    result.requiredLibraries.length should equal (0)
  }

  test("ingress image is not extracted when not present") {
    val content =
      """<section>
          |<p><strong>Du har sikkert opplevd rykter og usannheter</strong></p>
          |<ul>
          |<li><a href="#" title="Snopes">Snopes</a></li>
          |</ul>
        |</section>
      """.stripMargin.replace("\n", "")
    val expectedContentResult = """<section><ul><li><a href="#" title="Snopes">Snopes</a></li></ul></section>"""
    val expectedIngressResult = LanguageIngress(Some("Du har sikkert opplevd rykter og usannheter"), None)
    val (result, status) = HTMLCleaner.convert(defaultLanguageContent.copy(content=content), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
    result.requiredLibraries.length should equal (0)
  }

  test("ingress text is extracted when wrapped in <strong> tags") {
    val content = """<section>
                    |<figure data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-id="1" data-resource="image" data-alt="Mobiltelefon sender SMS"></figure>
                    |<strong>Medievanene er i endring.</strong>
                    |<h2>Mediehverdagen</h2>
                    |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult = """<section><h2>Mediehverdagen</h2></section>"""
    val expectedIngressResult = LanguageIngress(Some("Medievanene er i endring."), Some("http://image-api/images/5452"))
    val (result, status) = HTMLCleaner.convert(defaultLanguageContent.copy(content=content), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
  }
}
