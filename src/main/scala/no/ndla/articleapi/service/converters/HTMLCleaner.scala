package no.ndla.articleapi.service.converters

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties._
import no.ndla.articleapi.integration.{ConverterModule, LanguageContent, LanguageIngress}
import no.ndla.articleapi.model.domain.ImportStatus
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
      filter(x => !isTagValid(x.tagName))
      .map(x => {
        val tagName = x.tagName
        x.unwrap()
        tagName
      })
      .distinct
  }

  private def removeAttributes(el: Element): Seq[String] = {
    el.select("*").toList.flatMap(tag => {
      tag.attributes().toList.
        filter(attribute => !isAttributeKeyValid(attribute.getKey, tag.tagName))
        .map(illegalAttribute => {
          val keyName = illegalAttribute.getKey
          tag.removeAttr(keyName)
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
      if (el.select(resourceHtmlEmbedTag).isEmpty && !el.hasText && el.isBlock) {
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

  private def extractIngress(el: Element): (Option[LanguageIngress]) = {
    val ingressTextElement = getIngressText(el)

    val ingressText = ingressTextElement.map(rs => {
      rs.remove()
      rs.text()
    })

    removeEmptyTags(el)

    ingressText.map(text => LanguageIngress(text))

  }

  private object PermittedHTML {
    // MathML element reference list: https://developer.mozilla.org/en/docs/Web/MathML/Element
    private val mathJaxTags = Set("math", "maction", "maligngroup", "malignmark", "menclose", "merror", "mfenced", "mfrac", "mglyph", "mi",
      "mlabeledtr", "mlongdiv", "mmultiscripts", "mn", "mo", "mover", "mpadded", "mphantom", "mroot", "mrow", "ms", "mscarries",
      "mscarry", "msgroup", "msline", "mspace", "msqrt", "msrow", "mstack", "mstyle", "msub", "msup", "msubsup", "mtable", "mtd",
      "mtext", "mtr", "munder", "munderover", "semantics", "annotation", "annotation-xml")
    val tags = Set("body", "article", "section", "table", "tr", "td", "li", "a", "button", "div", "p", "pre", "code", "sup",
      "h1", "h2", "h3", "h4", "h5", "h6", "aside", "strong", "ul", "br", "ol", "i", "em", "b", "th", "tbody", "blockquote",
      "details", "summary", "table", "thead", "tfoot", "tbody", "caption", "audio", "figcaption", resourceHtmlEmbedTag) ++ mathJaxTags

    val legalAttributesForAll = Set("href", "title")
    val tagAttributes = Map(
      "td" -> Set("align", "valign"),
      "th" -> Set("align", "valign"),
      resourceHtmlEmbedTag -> Set("data-resource", "data-id", "data-content-id", "data-link-text", "data-url",
        "data-size", "data-videoid", "data-account", "data-player", "data-key", "data-alt", "data-caption", "data-align",
        "data-audio-id", "data-nrk-video-id", "data-message")
    )
  }

  def isAttributeKeyValid(attributeKey: String, tagName: String): Boolean = {
    val legalAttributesForTag = PermittedHTML.tagAttributes.getOrElse(tagName, Set())
    (legalAttributesForTag ++ PermittedHTML.legalAttributesForAll).contains(attributeKey)
  }

  def isTagValid(tagName: String): Boolean = {
    PermittedHTML.tags.contains(tagName)
  }

}
