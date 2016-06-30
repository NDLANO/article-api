package no.ndla.contentapi.service.converters

import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import no.ndla.contentapi.integration.{LanguageContent, NodeIngress}
import no.ndla.contentapi.model.{Author, Copyright, License}
import no.ndla.contentapi.service._
import org.mockito.Mockito._

class IngressConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val license = License("licence", "description", Some("http://"))
  val author = Author("forfatter", "Henrik")
  val copyright = Copyright(license, "", List(author))

  test("That an ingress is added to the content if the node contains an ingress") {
    val initialContent = """<article><div>Banankake</div></article>"""
    val ingressText = "<p>Introduksjon til banankake</p>"
    val expectedContent = s"<article> <section> $ingressText </section> <div> Banankake </div></article>"
    val node = LanguageContent(nodeId, nodeId, initialContent, Some("nb"))
    val ingressNode = NodeIngress(nodeId, "<p>Introduksjon til banankake</p>", None, 1)

    when(extractService.getNodeIngress(nodeId)).thenReturn(Some(ingressNode))
    val (result, status) = ingressConverter.convert(node)
    val strippedResult = " +".r.replaceAllIn(result.content.replace("\n", ""), " ")

    strippedResult should equal(expectedContent)
  }

  test("That an image is added to the ingress if the node contains an image") {
    val imageNid = "5678"
    val (small, full) = (Image("small.jpg", 1024, ""), Image("full.jpg", 1024, ""))
    val imageVariants = ImageVariants(Some(small), Some(full))
    val image = ImageMetaInformation("1234", List(ImageTitle("", Some("nb"))), List(ImageAltText("", Some("nb"))), imageVariants, copyright, List(ImageTag("", Some(""))))
    val initialContent = """<article><div>Banankake</div></article>"""
    val ingressText = "<p>Introduksjon til banankake</p>"
    val expectedContent = s"""<article> <section> <img src="/images/full.jpg" /> $ingressText </section> <div> Banankake </div></article>"""
    val node = LanguageContent(nodeId, nodeId, initialContent, Some("nb"))
    val ingressNode = NodeIngress(nodeId, "<p>Introduksjon til banankake</p>", Some(imageNid), 1)

    when(extractService.getNodeIngress(nodeId)).thenReturn(Some(ingressNode))
    when(imageApiService.importOrGetMetaByExternId(imageNid)).thenReturn(Some(image))
    val (result, status) = ingressConverter.convert(node)
    val strippedResult = " +".r.replaceAllIn(result.content.replace("\n", ""), " ")

    strippedResult should equal(expectedContent)
  }

  test("That the content remains unchanged if the node does not contain an ingress field") {
    val initialContent = """<article><div>Banankake</div></article>"""
    val node = LanguageContent(nodeId, nodeId, initialContent, Some("nb"))

    when(extractService.getNodeIngress(nodeId)).thenReturn(None)
    val (result, status) = ingressConverter.convert(node)

    result.content should equal(initialContent)
  }

  test("That an ingress is added to the content only if it should be used") {
    val initialContent = """<article><div>Banankake</div></article>"""
    val ingressText = "<p>Introduksjon til banankake</p>"
    val expectedContent = s"<article> <section> $ingressText </section> <div> Banankake </div></article>"
    val node = LanguageContent(nodeId, nodeId, initialContent, Some("nb"))
    val ingressNode = NodeIngress(nodeId, "<p>Introduksjon til banankake</p>", None, 0)

    when(extractService.getNodeIngress(nodeId)).thenReturn(Some(ingressNode))
    val (result, status) = ingressConverter.convert(node)

    result.content should equal(initialContent)
  }
}
