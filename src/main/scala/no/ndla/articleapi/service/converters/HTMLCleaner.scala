package no.ndla.articleapi.service.converters

import no.ndla.articleapi.ArticleApiProperties._
import no.ndla.articleapi.integration.{ConverterModule, LanguageContent, LanguageIngress}
import no.ndla.articleapi.model.ImportStatus
import org.jsoup.nodes.{Element, Node}
import scala.collection.JavaConversions._

object HTMLCleaner extends ConverterModule {
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
    for (el <- element.select("p,div")) {
      if (!el.hasText && el.isBlock) {
        el.remove()
      }
    }

    element
  }

  private def removeNbsp(el: Element) {
    el.html(el.html().replace("\u00a0", "")) // \u00a0 is the unicode representation of &nbsp;
  }

  private def getIngressText(el: Element): Element = {
    val ingress = el.select("body>section>p>strong")
    if (ingress.size > 0)
      ingress.parents.first
    else
      el.select("body>section>strong").first
  }

  private def extractIngress(el: Element): (Option[LanguageIngress]) = {
    val ingressTextElement = Option(getIngressText(el))
    val ingressImageElement = Option(el.select("body>section>figure[data-resource=image]"))

    val ingressText = ingressTextElement.flatMap(rs => {
      rs.remove()
      stringToOption(rs.outerHtml())
    })
    val ingressImage = ingressImageElement.flatMap(rs => {
      rs.remove()
      stringToOption(rs.attr("data-url"))
    })

    (ingressText, ingressImage) match {
      case (None, None) => None
      case _ => Some(LanguageIngress(ingressText, ingressImage))
    }
  }

  private def stringToOption(str: String): Option[String] = Option(str).filter(_.trim.nonEmpty)
}
