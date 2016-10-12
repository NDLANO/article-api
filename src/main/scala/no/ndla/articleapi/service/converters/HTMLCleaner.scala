package no.ndla.articleapi.service.converters

import no.ndla.articleapi.integration.{ConverterModule, LanguageContent}
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

    (content.copy(content=jsoupDocumentToString(element)),
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

  def isAttributeKeyValid(attributeKey: String, tagName: String): Boolean = {
    val legalAttributesForAll = Set("href", "title")
    val permittedHTMLAttributes = Map(
      "td" -> Set("align", "valign"),
      "th" -> Set("align", "valign"),
      "figure" -> Set("data-resource", "data-id", "data-content-id", "data-link-text", "data-url",
        "data-size", "data-videoid", "data-account", "data-player", "data-key", "data-alt", "data-caption", "data-align", "data-nrk-video-id")
    )

    val legalAttributesForTag = permittedHTMLAttributes.getOrElse(tagName, Set())
    (legalAttributesForTag ++ legalAttributesForAll).contains(attributeKey)
  }

  def isTagValid(tagName: String): Boolean = {
    // MathML element reference list: https://developer.mozilla.org/en/docs/Web/MathML/Element
    val mathJaxTags = Set("math", "maction", "maligngroup", "malignmark", "menclose", "merror", "mfenced", "mfrac", "mglyph", "mi",
      "mlabeledtr", "mlongdiv", "mmultiscripts", "mn", "mo", "mover", "mpadded", "mphantom", "mroot", "mrow", "ms", "mscarries",
      "mscarry", "msgroup", "msline", "mspace", "msqrt", "msrow", "mstack", "mstyle", "msub", "msup", "msubsup", "mtable", "mtd",
      "mtext", "mtr", "munder", "munderover", "semantics", "annotation", "annotation-xml")
    val permittedHTMLTags = Set("body", "article", "section", "table", "tr", "td", "li", "a", "button", "div", "p", "pre", "code", "sup",
      "h1", "h2", "h3", "h4", "h5", "h6", "aside", "strong", "figure", "ul", "br", "ol", "i", "em", "b", "th", "tbody", "blockquote",
      "details", "summary", "table", "thead", "tfoot", "tbody", "caption", "audio", "figcaption") ++ mathJaxTags
    permittedHTMLTags.contains(tagName)
  }

}
