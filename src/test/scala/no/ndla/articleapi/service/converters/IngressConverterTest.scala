/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.articleapi.integration.{LanguageContent}
import no.ndla.articleapi.model._
import no.ndla.articleapi.service._
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
    val ingressNode = NodeIngress("<p>Introduksjon til banankake</p>", None, 1)
    val node = LanguageContent(nodeId, nodeId, initialContent, Some("nb"), Some(ingressNode))

    val (result, status) = ingressConverter.convert(node, ImportStatus(Seq(), Seq()))
    val strippedResult = " +".r.replaceAllIn(result.content.replace("\n", ""), " ")

    strippedResult should equal(expectedContent)
  }

  test("That an image is added to the ingress if the node contains an image") {
    val imageNid = "5678"
    val (small, full) = (Image("small.jpg", 1024, ""), Image("full.jpg", 1024, ""))
    val imageVariants = ImageVariants(Some(small), Some(full))
    val image = ImageMetaInformation("1234", List(ImageTitle("", Some("nb"))), List(ImageAltText("", Some("nb"))), imageVariants, copyright, List(ImageTag(List(""), Some(""))))
    val initialContent = """<article><div>Banankake</div></article>"""
    val ingressText = "<p>Introduksjon til banankake</p>"
    val expectedContent = s"""<article> <section> <img src="/images/full.jpg" /> $ingressText </section> <div> Banankake </div></article>"""
    val ingressNode = NodeIngress("<p>Introduksjon til banankake</p>", Some(imageNid), 1)
    val node = LanguageContent(nodeId, nodeId, initialContent, Some("nb"), Some(ingressNode))

    when(imageApiService.importOrGetMetaByExternId(imageNid)).thenReturn(Some(image))
    val (result, status) = ingressConverter.convert(node, ImportStatus(Seq(), Seq()))
    val strippedResult = " +".r.replaceAllIn(result.content.replace("\n", ""), " ")

    strippedResult should equal(expectedContent)
  }

  test("That the content remains unchanged if the node does not contain an ingress field") {
    val initialContent = """<article><div>Banankake</div></article>"""
    val node = LanguageContent(nodeId, nodeId, initialContent, Some("nb"), None)

    val (result, status) = ingressConverter.convert(node, ImportStatus(Seq(), Seq()))

    result.content should equal(initialContent)
  }

  test("That an ingress is added to the content only if it should be used") {
    val initialContent = """<article><div>Banankake</div></article>"""
    val ingressText = "<p>Introduksjon til banankake</p>"
    val expectedContent = s"<article> <section> $ingressText </section> <div> Banankake </div></article>"
    val ingressNode = NodeIngress("<p>Introduksjon til banankake</p>", None, 0)
    val node = LanguageContent(nodeId, nodeId, initialContent, Some("nb"), Some(ingressNode))

    val (result, status) = ingressConverter.convert(node, ImportStatus(Seq(), Seq()))

    result.content should equal(initialContent)
  }
}