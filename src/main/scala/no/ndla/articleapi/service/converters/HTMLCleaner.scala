package no.ndla.articleapi.service.converters

import com.typesafe.scalalogging.LazyLogging
import no.ndla.validation.EmbedTagRules.ResourceHtmlEmbedTag
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.integration.{ConverterModule, ImageApiClient, LanguageContent, LanguageIngress}
import no.ndla.articleapi.model.domain.ImportStatus
import no.ndla.validation.{Attributes, HtmlRules, ResourceType}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, Node, TextNode}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.{Success, Try}

trait HTMLCleaner {
  this: ImageApiClient with HtmlTagGenerator =>
  val htmlCleaner: HTMLCleaner

  class HTMLCleaner extends ConverterModule with LazyLogging {
    override def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)] = {
      val element = stringToJsoupDocument(content.content)
      val illegalTags = unwrapIllegalTags(element).map(x => s"Illegal tag(s) removed: $x").distinct
      convertLists(element)
      val illegalAttributes = removeAttributes(element).map(x => s"Illegal attribute(s) removed: $x").distinct

      moveEmbedsOutOfPTags(element)
      removeComments(element)
      removeNbsp(element)
      wrapStandaloneTextInPTag(element)
      // Jsoup doesn't support removing elements while iterating the dom-tree.
      // Thus executes the routine 3 times in order to be sure to remove all tags
      (1 to 3).foreach(_ => removeEmptyTags(element))

      val metaDescription = prepareMetaDescription(content.metaDescription)
      mergeTwoFirstSectionsIfFeasible(element)
      val ingress = getIngress(content, element)

      moveMisplacedAsideTags(element)
      unwrapDivsAroundDetailSummaryBox(element)
      unwrapDivsInAsideTags(element)

      convertH3sToH2s(element)
      val finalCleanedDocument = allContentMustBeWrappedInSectionBlocks(element)

      Success((content.copy(content = jsoupDocumentToString(finalCleanedDocument), metaDescription = metaDescription, ingress = ingress),
        importStatus.addMessages(illegalTags ++ illegalAttributes)))
    }

    private def convertH3sToH2s(element: Element) {
      if (element.select("h2").size() == 0)
        element.select("h3").asScala.foreach(_.tagName("h2"))
    }

    private def unwrapDivsAroundDetailSummaryBox(element: Element) {
      @tailrec
      def unwrapNestedDivs(detailsElem: Element) {
        if (detailsElem.parent.tagName == "div" && detailsElem.siblingElements.size == 0) {
          detailsElem.parent.unwrap()
          unwrapNestedDivs(detailsElem)
        }
      }

      element.select("details").asScala.foreach(unwrapNestedDivs)
    }

    private def moveEmbedsOutOfPTags(element: Element) {
      val embedsThatShouldNotBeInPTags = Set(
        ResourceType.Audio,
        ResourceType.Brightcove,
        ResourceType.ExternalContent,
        ResourceType.KhanAcademy,
        ResourceType.Image
      )

      val embedTypeString = embedsThatShouldNotBeInPTags.map(t => s"[${Attributes.DataResource}=$t]").mkString(",")

      element.select("p").asScala.foreach(pTag => {
        pTag.select(s"${ResourceHtmlEmbedTag}${embedTypeString}").asScala.toList.foreach(el => {
          pTag.before(el.outerHtml())
          el.remove()
        })
      })
    }

    private def mergeTwoFirstSectionsIfFeasible(el: Element) {
      val sections = el.select("section").asScala

      if (sections.size < 2)
        return

      val firstSectionChildren = sections.head.children
      if (firstSectionChildren.size != 1 || firstSectionChildren.asScala.head.children.size > 2)
        return

      firstSectionChildren.select(ResourceHtmlEmbedTag).asScala.headOption match {
        case Some(e) =>
          sections(1).prepend(e.outerHtml())
          e.remove()
          sections.head.childNodeSize() match {
            case x if x == 0 => sections.head.remove()
            case _ =>
          }
        case _ =>
      }
    }

    private def getIngress(content: LanguageContent, element: Element): Option[LanguageIngress] = {
      content.ingress match {
        case None => extractIngress(element).map(LanguageIngress(_, None))
        case Some(ingress) =>
          val imageEmbedHtml = ingress.ingressImage.flatMap(imageApiClient.importOrGetMetaByExternId)
            .map(imageMetaData => HtmlTagGenerator.buildImageEmbedContent(
              caption = "",
              imageId = imageMetaData.id.toString,
              align = "",
              size = "",
              altText = imageMetaData.alttexts.find(_.language == content.language).map(_.alttext).getOrElse("")))

          imageEmbedHtml.map(element.prepend)

          Some(LanguageIngress(extractElement(stringToJsoupDocument(ingress.content)), None))
      }
    }

    private def unwrapIllegalTags(el: Element): Seq[String] = {
      el.children().select("*").asScala.toList
        .filter(htmlTag => !HtmlRules.isTagValid(htmlTag.tagName))
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
      Jsoup.parseBodyFragment(extractElement(element)).body().html().replace("&nbsp;", " ").trim
    }

    private def removeAttributes(el: Element): Seq[String] = {
      el.select("*").asScala.toList.flatMap(tag =>
        HtmlRules.removeIllegalAttributes(tag, HtmlRules.legalAttributesForTag(tag.tagName))
      )
    }

    private def removeComments(node: Node) {
      var i = 0

      while (i < node.childNodes().size()) {
        val child = node.childNode(i)

        child.nodeName() == "#comment" match {
          case true => child.remove()
          case false => {
            i += 1
            removeComments(child)
          }
        }
      }
    }

    private def htmlTagIsEmpty(el: Element) = {
      el.select(ResourceHtmlEmbedTag).isEmpty && el.select("math").isEmpty && !el.hasText
    }

    private def removeEmptyTags(element: Element): Element = {
      val tagsToRemove = Set("p", "div", "section", "aside", "strong")
      for (el <- element.select(tagsToRemove.mkString(",")).asScala) {
        if (htmlTagIsEmpty(el)) {
          el.remove()
        }
      }

      element
    }

    // Since jsoup does not provide a way to remove &nbsp; from a tag, but not its children
    // We first replace it with a placeholder to then replace replace the placeholder with &nbsp;
    // in tags where nbsp's are allowed.
    private def removeNbsp(el: Element) {
      el.select("*").select("mo").asScala.foreach(mo => if (mo.html().equals(NBSP)) mo.html("[mathspace]"))
      el.html(el.html().replace(NBSP, " "))
      el.select("*").select("mo").asScala.foreach(mo => if (mo.html().equals("[mathspace]")) mo.html(NBSP))
    }

    // A paragraph containing an ingress can also be split up into mulitple strong-tags
    // e.g <p><strong>first</strong> <em><strong>second</strong></em></p>.
    // returns a sequence of all ingress elements
    private def getAllIngressElements(el: Element): Seq[Element] = {
      val paragraph = if (el.tagName == "p") el else el.parent

      if (paragraph.select("strong").text == paragraph.text)
        paragraph.select("strong").asScala
      else
        Seq(el)
    }

    private def getIngressText(el: Element): Option[Seq[Element]] = {
      val firstParagraphs = Option(el.select(">p")).map(_.asScala.toList)
        .flatMap(paragraphs => paragraphs.headOption.map(_ => paragraphs.take(2))) // select two first paragraphs
      val ingress = firstParagraphs.flatMap(ps => {
        val ingresses = ps.map(p => Option(p.select(">strong").first))

        // In some cases the ingress is split up into two paragraphs
        ingresses match {
          case Some(head) :: Some(second) :: _ => Some(getAllIngressElements(head) ++ getAllIngressElements(second))
          case Some(head) :: None :: _ => Some(getAllIngressElements(head))
          case Some(head) :: _ => Some(getAllIngressElements(head))
          case None :: Some(second) :: _ =>
            ps.head.select(">embed").first match {
              case _: Element => Some(getAllIngressElements(second))
              case _ => None
            }

          case _ => None
        }
      })

      ingress match {
        case None => Option(el.select(">strong:eq(0)").first).orElse(Option(el.select(">strong:eq(1)").first)).map(Seq(_))
        case x => x
      }
    }

    private def extractIngress(el: Element): Option[String] = {
      val minimumIngressWordCount = 3
      val firstSection = Option(el.select("body>section").first)
      val firstDivSection = Option(el.select("body>section:eq(0)>div").first)
      val secondDivSection = Option(el.select("body>section:eq(0)>div:eq(0)>div:nth-child(1)").first)

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

    val NodeTypesToGroupTogether = "em" :: "#text" :: "math" :: "strong" :: Nil

    private def wrapThingsInP(nodes: Seq[Node]) {
      val grouped = new Element("p")

      val firstNonTextElementIdx = nodes.indexWhere(n => !NodeTypesToGroupTogether.contains(n.nodeName()) && n.toString.trim.length > 0) match {
        case idx: Int if idx < 0 => nodes.length
        case idx => idx
      }

      val toBeWrapped = nodes.slice(0, firstNonTextElementIdx)
      toBeWrapped.foreach(child => grouped.appendChild(child.clone))
      toBeWrapped.drop(1).foreach(_.remove())
      nodes.headOption.foreach(_.replaceWith(grouped))
    }

    def wrapStandaloneTextInPTag(element: Element): Element = {
      val sections = element.select("body>section").asScala

      sections.foreach(section => {
        def firstTextNodeIdx: Int =
          section.childNodes.asScala.indexWhere(n => NodeTypesToGroupTogether.contains(n.nodeName()))

        while (firstTextNodeIdx > -1) {
          val childNodes = section.childNodes().asScala
          wrapThingsInP(childNodes.drop(firstTextNodeIdx))
        }
      })

      element
    }

    private def unwrapDivsInAsideTags(element: Element): Element = {
      val asides = element.select("body>section>aside").asScala
      for (aside <- asides) {
        if (aside.children.size == 1)
          unwrapNestedDivs(aside.children.first)
      }
      element
    }

    private def unwrapNestedDivs(child: Element) {
      if (child.tagName() == "div" && child.siblingElements.size == 0) {
        val firstChild = child.children.first
        child.unwrap()
        unwrapNestedDivs(firstChild)
      }
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

    private def allContentMustBeWrappedInSectionBlocks(element: Element): Element = {
      val body = element.select("body").first
      if (body.childNodeSize() < 1)
        return element

      val rootLevelBlocks = body.children
      if (rootLevelBlocks.select("section").isEmpty) {
        return stringToJsoupDocument(s"<section>${body.outerHtml}</section>")
      }

      if (rootLevelBlocks.first().tagName() != "section") {
        body.prepend("<section></section>")
      }

      rootLevelBlocks.asScala.foreach {
        case el if el.tagName != "section" =>
          el.previousElementSibling.append(el.outerHtml)
          el.remove()
        case _ =>
      }
      element
    }

    private def convertLists(element: Element) = {
      element.select("ol").asScala.foreach(x => {
        val styling = x.attr("style").split(";")
        if (styling.contains("list-style-type: lower-alpha")) {
          x.attr(Attributes.DataType.toString, "letters")
        }
      })
    }

  }

}

