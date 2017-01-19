package no.ndla.articleapi.service.converters

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties._
import no.ndla.articleapi.integration.{ConverterModule, ImageApiClient, LanguageContent, LanguageIngress}
import no.ndla.articleapi.model.domain.ImportStatus
import org.jsoup.nodes.{Element, Node, TextNode}
import no.ndla.articleapi.integration.ConverterModule.{stringToJsoupDocument, jsoupDocumentToString}

import scala.collection.JavaConversions._

trait HTMLCleaner {
  this: ImageApiClient with HtmlTagGenerator =>
  val htmlCleaner: HTMLCleaner

  class HTMLCleaner extends ConverterModule with LazyLogging {

    override def convert(content: LanguageContent, importStatus: ImportStatus): (LanguageContent, ImportStatus) = {
      val element = stringToJsoupDocument(content.content)
      val illegalTags = unwrapIllegalTags(element).map(x => s"Illegal tag(s) removed: $x").distinct
      val illegalAttributes = removeAttributes(element).map(x => s"Illegal attribute(s) removed: $x").distinct

      moveImagesOutOfPTags(element)
      removeComments(element)
      removeNbsp(element)
      removeEmptyTags(element)
      wrapStandaloneTextInPTag(element)

      val metaDescription = prepareMetaDescription(content.metaDescription)
      val ingress = getIngress(content, element)

      (content.copy(content=jsoupDocumentToString(element), ingress=ingress, metaDescription=metaDescription),
        ImportStatus(importStatus.messages ++ illegalTags ++ illegalAttributes, importStatus.visitedNodes))
    }

    private def moveImagesOutOfPTags(element: Element) {
      for (el <- element.select("p").select(s"""$resourceHtmlEmbedTag[data-resource=image]""")) {
        el.parent.before(el.outerHtml())
        el.remove()
      }
    }

    private def getIngress(content: LanguageContent, element: Element): Option[LanguageIngress] = {
      content.ingress match {
        case None => extractIngress(element).map(LanguageIngress(_, None))
        case Some(ingress) =>
          val (imageEmbedHtml) = ingress.ingressImage.flatMap(imageApiClient.importOrGetMetaByExternId)
            .map(imageMetaData => HtmlTagGenerator.buildImageEmbedContent(
              caption="",
              imageId=imageMetaData.id.toString,
              align="",
              size="",
              altText=imageMetaData.alttexts.find(_.language==content.language).map(_.alttext).getOrElse("")))

          imageEmbedHtml.map(x => element.prepend(x._1))

          Some(LanguageIngress(extractElement(stringToJsoupDocument(ingress.content)), None))
      }
    }

    private def unwrapIllegalTags(el: Element): Seq[String] = {
      el.children().select("*").toList
        .filter(htmlTag => !HTMLCleaner.isTagValid(htmlTag.tagName))
        .map(illegalHtmlTag => {
          val tagName = illegalHtmlTag.tagName
          illegalHtmlTag.unwrap()
          tagName
        })
        .distinct
    }

    private def prepareMetaDescription(metaDescription: String): String = {
      val element = stringToJsoupDocument(metaDescription)
      for (el <- element.select("embed")) {
        val caption = el.attr("data-caption")
        el.replaceWith(new TextNode(caption, ""))
      }
      extractElement(element)
    }

    private def removeAttributes(el: Element): Seq[String] = {
      el.select("*").toList.flatMap(tag => {
        tag.attributes().toList.
          filter(attribute => !HTMLCleaner.isAttributeKeyValid(attribute.getKey, tag.tagName))
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

    private def htmlTagIsEmpty(el: Element) = {
      el.select(resourceHtmlEmbedTag).isEmpty && !el.hasText && el.isBlock
    }

    private def removeEmptyTags(element: Element): Element = {
      for (el <- element.select("p,div,section,aside")) {
        if (htmlTagIsEmpty(el)) {
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

    private def extractIngress(el: Element): Option[String] = {
      val ingressText = getIngressText(el).map(ingress => extractElement(ingress))
      removeEmptyTags(el)
      ingressText
    }

    private def extractElement(elementToExtract: Element): String = {
      elementToExtract.remove()
      elementToExtract.text()
    }

    private def wrapStandaloneTextInPTag (element: Element) : Element = {
      val sections = element.select("body>section")
      sections.map(node => node.childNodes().map(child => {
        if (child.nodeName() == "#text" && !child.asInstanceOf[TextNode].isBlank) {
          child.wrap("<p>")
        }
        child
      }))

      element
    }
  }

}

object HTMLCleaner {
  private object PermittedHTML {
    // MathML element reference list: https://developer.mozilla.org/en/docs/Web/MathML/Element
    private val mathJaxTags = Set("math", "maction", "maligngroup", "malignmark", "menclose", "merror", "mfenced", "mfrac", "mglyph", "mi",
      "mlabeledtr", "mlongdiv", "mmultiscripts", "mn", "mo", "mover", "mpadded", "mphantom", "mroot", "mrow", "ms", "mscarries",
      "mscarry", "msgroup", "msline", "mspace", "msqrt", "msrow", "mstack", "mstyle", "msub", "msup", "msubsup", "mtable", "mtd",
      "mtext", "mtr", "munder", "munderover", "semantics", "annotation", "annotation-xml")
    val tags = Set("article", "section", "tr", "td", "li", "a", "button", "div", "p", "pre", "code", "sup",
      "h1", "h2", "h3", "h4", "h5", "h6", "aside", "strong", "ul", "br", "ol", "i", "em", "b", "th", "blockquote",
      "details", "summary", "table", "thead", "tfoot", "tbody", "caption", "audio", "figcaption", resourceHtmlEmbedTag) ++ mathJaxTags

    val legalAttributesForAll = Set("href", "title")
    val tagAttributes = Map(
      "td" -> Set("align", "valign"),
      "th" -> Set("align", "valign"),
      resourceHtmlEmbedTag -> Set("data-resource", "data-resource_id", "data-id", "data-content-id", "data-link-text", "data-url",
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
