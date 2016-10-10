package no.ndla.articleapi.service.converters

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties._
import no.ndla.articleapi.integration.{ConverterModule, LanguageContent, LanguageIngress}
import no.ndla.articleapi.model.ImportStatus
import org.jsoup.nodes.{Element, Node}

import scala.collection.JavaConversions._

object HTMLCleaner extends ConverterModule with LazyLogging {
  override def convert(content: LanguageContent, importStatus: ImportStatus): (LanguageContent, ImportStatus) = {
    val element = stringToJsoupDocument(content.content)

    val illegalTags = unwrapIllegalTags(element).map(x => s"Illegal tag(s) removed: $x").distinct
    val illegalAttributes = removeAttributes(element).map(x => s"Illegal attribute(s) removed: $x").distinct
    removeComments(element)
    removeNbsp(element)
    removeEmptyTags(element)
    val ingress = extractIngress(element)

    (content.copy(content=jsoupDocumentToString(element), ingress=ingress),
      ImportStatus(importStatus.messages ++ illegalTags ++ illegalAttributes, importStatus.visitedNodes))
  }

  private def unwrapIllegalTags(el: Element): Seq[String] = {
    el.select("*").toList.
      filter(x => !permittedHTMLTags.contains(x.tagName))
      .map(x => {
        val tagName = x.tagName
        x.unwrap()
        tagName
      })
      .distinct
  }

  private def removeAttributes(el: Element): Seq[String] = {
    el.select("*").toList.flatMap(x => {
      x.attributes().toList.
        filter(x => !permittedHTMLAttributes.contains(x.getKey))
        .map(y => {
          val keyName = y.getKey
          x.removeAttr(keyName)
          keyName
      })
    })
  }

  private def removeComments(node: Node) {
    var i = 0

    while (i < node.childNodes().size()) {
      val child = node.childNode(i)

      child.nodeName() == "#comment" match {
        case true => child.remove()
        case false => {
          i+= 1
          removeComments(child)
        }
      }
    }
  }

  private def removeEmptyTags(element: Element): Element = {
    for (el <- element.select("p,div,section")) {
      if (!el.hasText && el.isBlock) {
        el.remove()
      }
    }

    element
  }

  private def removeNbsp(el: Element) {
    el.html(el.html().replace("\u00a0", "")) // \u00a0 is the unicode representation of &nbsp;
  }

  private def getIngressText(el: Element): Option[Element] = {
    val firstSection = Option(el.select("body>section").first)
    val ingress = firstSection.flatMap(section => Option(section.select(">p>strong").first))

    ingress match {
      case None => firstSection.flatMap(section => Option(section.select(">strong").first))
      case x => x
    }
  }

  private def getIngressImage(el: Element): Option[Element] = {
    val firstSection = Option(el.select("body>section").first)
    firstSection.flatMap(section => Option(section.select("figure[data-resource=image]").first))
  }

  private def extractIngress(el: Element): (Option[LanguageIngress]) = {
    val ingressTextElement = getIngressText(el)
    val ingressImageElement = getIngressImage(el)

    val ingressText = ingressTextElement.flatMap(rs => {
      rs.remove()
      stringToOption(rs.text)
    })
    val ingressImageUrl = ingressImageElement.flatMap(rs => {
      rs.remove()
      stringToOption(rs.attr("data-url"))
    })

    removeEmptyTags(el)
    (ingressText, ingressImageUrl) match {
      case (None, None) => None
      case _ => Some(LanguageIngress(ingressText, ingressImageUrl))
    }
  }

  private def stringToOption(str: String): Option[String] = Option(str).filter(_.trim.nonEmpty)
}
