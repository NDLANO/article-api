package no.ndla.articleapi.service.converters

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties._
import no.ndla.articleapi.integration.{ConverterModule, ImageApiClient, LanguageContent, LanguageIngress}
import no.ndla.articleapi.model.domain.ImportStatus
import org.jsoup.nodes.{Element, Node, TextNode}
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import Attributes._
import org.json4s.native.JsonMethods.parse
import org.json4s._

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Success, Try}

trait HTMLCleaner {
  this: ImageApiClient with HtmlTagGenerator =>
  val htmlCleaner: HTMLCleaner

  class HTMLCleaner extends ConverterModule with LazyLogging {

    override def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)] = {
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

      moveMisplacedAsideTags(element)

      Success((content.copy(content=jsoupDocumentToString(element), ingress=ingress, metaDescription=metaDescription),
        ImportStatus(importStatus.messages ++ illegalTags ++ illegalAttributes, importStatus.visitedNodes)))
    }

    private def moveImagesOutOfPTags(element: Element) {
      element.select("p").asScala.foreach(pTag => {
        pTag.select(s"""$resourceHtmlEmbedTag[$DataResource=image]""").asScala.toList.foreach(el => {
          pTag.before(el.outerHtml())
          el.remove()
        })
      })
    }

    private def getIngress(content: LanguageContent, element: Element): Option[LanguageIngress] = {
      content.ingress match {
        case None => extractIngress(element).map(LanguageIngress(_, content.language))
        case Some(ingress) =>
          val imageEmbedHtml = ingress.ingressImage.flatMap(imageApiClient.importOrGetMetaByExternId)
            .map(imageMetaData => HtmlTagGenerator.buildImageEmbedContent(
              caption="",
              imageId=imageMetaData.id.toString,
              align="",
              size="",
              altText=imageMetaData.alttexts.find(_.language==content.language).map(_.alttext).getOrElse("")))

          imageEmbedHtml.map(html => element.prepend(html))

          Some(LanguageIngress(extractElement(stringToJsoupDocument(ingress.content)), None))
      }
    }

    private def unwrapIllegalTags(el: Element): Seq[String] = {
      el.children().select("*").asScala.toList
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
      for (el <- element.select("embed").asScala) {
        val caption = el.attr("data-caption")
        el.replaceWith(new TextNode(caption, ""))
      }
      extractElement(element)
    }

    private def removeAttributes(el: Element): Seq[String] = {
      el.select("*").asScala.toList.flatMap(tag =>
        HTMLCleaner.removeIllegalAttributes(tag, HTMLCleaner.legalAttributesForTag(tag.tagName))
      )
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
      el.select(resourceHtmlEmbedTag).isEmpty && !el.hasText
    }

    private def removeEmptyTags(element: Element): Element = {
      for (el <- element.select("p,div,section,aside,strong").asScala) {
        if (htmlTagIsEmpty(el)) {
          el.remove()
        }
      }

      element
    }

    private def removeNbsp(el: Element) {
      el.html(el.html().replace("\u00a0", "")) // \u00a0 is the unicode representation of &nbsp;
    }

    private def getIngressText(el: Element): Option[Seq[Element]] = {
      val firstParagraphs = Option(el.select(">p")).map(_.asScala.toList)
        .flatMap(paragraphs => paragraphs.headOption.map(_ => paragraphs.take(2))) // select two first paragraphs

      val ingress = firstParagraphs.flatMap(ps => {
        val ingresses = ps.map(p => Option(p.select(">strong").first))

        // In some cases the ingress is split up into two paragraphs
        ingresses match {
          case Some(head) :: Some(second) :: _ => Some(Seq(head, second))
          case Some(head) :: None :: _ => Some(Seq(head))
          case Some(head) :: _ => Some(Seq(head))
          case _ => None
        }
      })

      ingress match {
        case None => Option(el.select(">strong").first).map(x => Seq(x))
        case x => x
      }
    }

    private def extractIngress(el: Element): Option[String] = {
      val minimumIngressWordCount = 3
      val firstSection = Option(el.select("body>section").first)
      val firstDivSection = Option(el.select("body>section:eq(0)>div").first)
      val secondDivSection = Option(el.select("body>section:eq(0)>div:eq(0)>div").first)

      // Look for ingress according to the following priorities:
      //   1. first paragraph in first section, ei. <section><p> HERE </p></section>
      //   2. first paragraph in first nested div inside first section, ei. <section><div><div><p> HERE </p></div></div></section>
      //   3. first paragraph in first div inside first section, ei. <section><div><p> HERE </p></div></section>
      val ingress = (firstSection.flatMap(getIngressText), firstDivSection, secondDivSection) match {
        case (Some(ing), _, _) => Some(ing)
        case (None, _, Some(secondDiv)) => getIngressText(secondDiv)
        case (None, Some(firstDiv), _) => getIngressText(firstDiv)
        case _ => None
      }

      def getText(elements: Seq[Element]): String = elements.map(_.text).mkString(" ")

      ingress match {
        case None => None
        case Some(ing) if getText(ing).split(" +").length >= minimumIngressWordCount =>
          val ingressText = ing.map(extractElement).mkString(" ")
          removeEmptyTags(el)
          Some(ingressText)
        case _ => None
      }
    }

    private def extractElement(elementToExtract: Element): String = {
      elementToExtract.remove()
      elementToExtract.text()
    }

    private def wrapStandaloneTextInPTag(element: Element): Element = {
      val sections = element.select("body>section").asScala
      sections.map(node => node.childNodes().asScala.map(child => {
        if (child.nodeName() == "#text" && !child.asInstanceOf[TextNode].isBlank) {
          child.wrap("<p>")
        }
        child
      }))

      element
    }

    private def moveMisplacedAsideTags(element: Element) = {
      val aside = element.select("body>section:eq(0)>aside:eq(0)").asScala.headOption
      aside match {
        case None =>
        case Some(e) =>
          val sibling = e.siblingElements().asScala.lift(0)
          sibling.map(s =>
            s.before(e)
          )
      }
      element
    }

  }
}

object HTMLCleaner {
  object PermittedHTML {
    val attributes: Map[String, Seq[String]] = readAttributes
    val tags: Set[String] = readTags

    private def convertJsonStr(jsonStr: String): Map[String, Any] = {
      implicit val formats = org.json4s.DefaultFormats
      parse(jsonStr).extract[Map[String, Any]]
    }
    private def htmlRulesJson: Map[String, Any] = convertJsonStr(Source.fromResource("html-rules.json").mkString)
    private def mathMLRulesJson: Map[String, Any] = convertJsonStr(Source.fromResource("mathml-rules.json").mkString)

    private def readAttributes: Map[String, Seq[String]] = {
      val htmlJson: Map[String, Any] = htmlRulesJson
      val mathMlJson: Map[String, Any] = mathMLRulesJson

      val htmlAttr = htmlJson.get("attributes").map(_.asInstanceOf[Map[String, Seq[String]]])
      val mathMlAttrs = mathMlJson.get("attributes").map(_.asInstanceOf[Map[String, Seq[String]]])
      htmlAttr.getOrElse(Map.empty) ++ mathMlAttrs.getOrElse(Map.empty)
    }

    private def readTags: Set[String] = {
      val htmlJson: Map[String, Any] = htmlRulesJson
      val mathMlJson: Map[String, Any] = mathMLRulesJson

      val htmlTags = htmlJson.get("tags").map(_.asInstanceOf[Seq[String]].toSet)
      val mathMlTags = mathMlJson.get("tags").map(_.asInstanceOf[Seq[String]].toSet)

      htmlTags.getOrElse(Set.empty) ++ mathMlTags.getOrElse(Set.empty) ++ attributes.keys
    }
  }

  def isAttributeKeyValid(attributeKey: String, tagName: String): Boolean = {
    val legalAttrs = legalAttributesForTag(tagName)
    legalAttrs.contains(attributeKey)
  }

  def isTagValid(tagName: String): Boolean = PermittedHTML.tags.contains(tagName)

  def allLegalTags: Set[String] = PermittedHTML.tags

  def legalAttributesForTag(tagName: String): Set[String] = {
    PermittedHTML.attributes.getOrElse(tagName, Seq.empty).toSet
  }

  def removeIllegalAttributes(el: Element, legalAttributes: Set[String]): Seq[String] = {
    el.attributes().asScala.toList.
      filter(attr => !legalAttributes.contains(attr.getKey))
      .map(illegalAttribute => {
        val keyName = illegalAttribute.getKey
        el.removeAttr(keyName)
        keyName
      })
  }
}
