/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import java.util.Map.Entry

import com.google.gson.{JsonElement, JsonObject}
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{SearchResult => JestSearchResult}
import no.ndla.articleapi.ArticleApiProperties.{maxConvertionRounds, nodeTypeBegrep}
import no.ndla.articleapi.auth.User
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.integration.ImageApiClient
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.{ArticleSummary, ArticleSummaryV2}
import no.ndla.articleapi.model.domain.Language._
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.converters.{Attributes, HTMLCleaner, ResourceType}
import no.ndla.mapping.License.getLicense
import no.ndla.network.ApplicationUrl

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: ConverterModules with ExtractConvertStoreContent with ImageApiClient with Clock with ArticleRepository with User =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {

    def getHits(response: JestSearchResult): Seq[ArticleSummary] = {
      var resultList = Seq[ArticleSummary]()
      response.getTotal match {
        case count: Integer if count > 0 => {
          val resultArray = response.getJsonObject.get("hits").asInstanceOf[JsonObject].get("hits").getAsJsonArray
          val iterator = resultArray.iterator()
          while (iterator.hasNext) {
            resultList = resultList :+ hitAsArticleSummary(iterator.next().asInstanceOf[JsonObject].get("_source").asInstanceOf[JsonObject])
          }
          resultList
        }
        case _ => Seq()
      }
    }

    def hitAsArticleSummary(hit: JsonObject): ArticleSummary = {
      ArticleSummary(
        hit.get("id").getAsString,
        hit.get("title").getAsJsonObject.entrySet.asScala.to[Seq].map(entr => api.ArticleTitle(entr.getValue.getAsString, entr.getKey)),
        hit.get("visualElement").getAsJsonObject.entrySet.asScala.to[Seq].map(entr => api.VisualElement(entr.getValue.getAsString, entr.getKey)),
        hit.get("introduction").getAsJsonObject.entrySet.asScala.to[Seq].map(entr => api.ArticleIntroduction(entr.getValue.getAsString, entr.getKey)),
        ApplicationUrl.get + hit.get("id").getAsString,
        hit.get("license").getAsString,
        hit.get("articleType").getAsString
      )
    }

    def getHitsV2(response: JestSearchResult, language: String): Seq[ArticleSummaryV2] = {
      var resultList = Seq[ArticleSummaryV2]()
      response.getTotal match {
        case count: Integer if count > 0 => {
          val resultArray = response.getJsonObject.get("hits").asInstanceOf[JsonObject].get("hits").getAsJsonArray
          val iterator = resultArray.iterator()
          while (iterator.hasNext) {
            resultList = resultList :+ hitAsArticleSummaryV2(iterator.next().asInstanceOf[JsonObject].get("_source").asInstanceOf[JsonObject], language)
          }
          resultList
        }
        case _ => Seq()
      }
    }

    def hitAsArticleSummaryV2(hit: JsonObject, language: String): ArticleSummaryV2 = {
      val titles = getEntrySetSeq(hit, "title").map(entr => ArticleTitle(entr.getValue.getAsString, entr.getKey))
      val introductions = getEntrySetSeq(hit, "introduction").map(entr => ArticleIntroduction (entr.getValue.getAsString, entr.getKey))
      val visualElements = getEntrySetSeq(hit, "visualElement").map(entr => VisualElement(entr.getValue.getAsString, entr.getKey))

      val supportedLanguages =  getSupportedLanguages(Seq(titles, visualElements, introductions))

      val title = findByLanguageOrBestEffort(titles, language).map(converterService.toApiArticleTitle).getOrElse(api.ArticleTitle("", DefaultLanguage))
      val visualElement = findByLanguageOrBestEffort(visualElements, language).map(converterService.toApiVisualElement)
      val introduction = findByLanguageOrBestEffort(introductions, language).map(converterService.toApiArticleIntroduction)

      ArticleSummaryV2(
        hit.get("id").getAsLong,
        title,
        visualElement,
        introduction,
        ApplicationUrl.get + hit.get("id").getAsString,
        hit.get("license").getAsString,
        hit.get("articleType").getAsString,
        supportedLanguages
      )
    }

    def getEntrySetSeq(hit: JsonObject, fieldPath: String): Seq[Entry[String, JsonElement]] = {
      hit.get(fieldPath).getAsJsonObject.entrySet.asScala.to[Seq]
    }

    def getValueByFieldAndLanguage(hit: JsonObject, fieldPath: String, searchLanguage: String): String = {
      hit.get(fieldPath).getAsJsonObject.entrySet.asScala.to[Seq].find(entr => entr.getKey == searchLanguage) match {
        case Some(element) => element.getValue.getAsString
        case None => ""
      }
    }

    def toDomainArticle(nodeToConvert: NodeToConvert, importStatus: ImportStatus): Try[(Content, ImportStatus)] = {
      val updatedVisitedNodes = importStatus.visitedNodes ++ nodeToConvert.contents.map(_.nid)

      convert(nodeToConvert, maxConvertionRounds, importStatus.copy(visitedNodes = updatedVisitedNodes.distinct))
          .flatMap { case (content, status) => postProcess(content, status) } match {
        case Failure(f) => Failure(f)
        case Success((convertedContent, converterStatus)) if convertedContent.nodeType == nodeTypeBegrep =>
          Success((toDomainConcept(convertedContent), converterStatus))
        case Success((convertedContent, converterStatus)) => Success((toDomainArticle(convertedContent), converterStatus))
      }
    }

    @tailrec private def convert(nodeToConvert: NodeToConvert, maxRoundsLeft: Int, importStatus: ImportStatus): Try[(NodeToConvert, ImportStatus)] = {
      if (maxRoundsLeft == 0) {
        val message = "Maximum number of converter rounds reached; Some content might not be converted"
        logger.warn(message)
        return Success((nodeToConvert, importStatus.copy(messages=importStatus.messages :+ message)))
      }

      val (updatedContent, updatedStatus) = executeConverterModules(nodeToConvert, importStatus) match {
        case Failure(e) => return Failure(e)
        case Success(s) => s
      }

      // If this converting round did not yield any changes to the content, this node is finished (case true)
      // If changes were made during this convertion, we run the converters again (case false)
      updatedContent == nodeToConvert match {
        case true => Success((updatedContent, updatedStatus))
        case false => convert(updatedContent, maxRoundsLeft - 1, updatedStatus)
      }
    }

    private def postProcess(nodeToConvert: NodeToConvert, importStatus: ImportStatus): Try[(NodeToConvert, ImportStatus)] =
      executePostprocessorModules(nodeToConvert, importStatus)


    private[service] def toDomainArticle(nodeToConvert: NodeToConvert): Article = {
      val requiredLibraries = nodeToConvert.contents.flatMap(_.requiredLibraries).distinct
      val ingresses = nodeToConvert.contents.flatMap(content => content.asArticleIntroduction)
      val visualElements = nodeToConvert.contents.flatMap(_.asVisualElement)

      val languagesInNode: Set[String] = (nodeToConvert.titles.map(_.language) ++
        nodeToConvert.contents.map(_.language) ++
        ingresses.map(_.language)).toSet

      Article(None,
        None,
        nodeToConvert.titles,
        nodeToConvert.contents.map(_.asContent),
        toDomainCopyright(nodeToConvert.license, nodeToConvert.authors),
        nodeToConvert.tags.filter(tag => languagesInNode.contains(tag.language)),
        requiredLibraries,
        visualElements,
        ingresses,
        nodeToConvert.contents.map(_.asArticleMetaDescription),
        None,
        nodeToConvert.created,
        nodeToConvert.updated,
        "content-import-client",
        nodeToConvert.articleType.toString
      )
    }

    private def toDomainConcept(convertedNode: NodeToConvert): Concept = {
      Concept(
        None,
        convertedNode.titles.map(title => ConceptTitle(title.title, title.language)),
        convertedNode.contents.map(content => ConceptContent(content.content, content.language)),
        convertedNode.authors,
        convertedNode.created,
        convertedNode.updated
      )
    }

    private def toDomainCopyright(license: String, authors: Seq[Author]): Copyright = {
      val origin = authors.find(author => author.`type`.toLowerCase == "opphavsmann").map(_.name).getOrElse("")
      val authorsExcludingOrigin = authors.filterNot(x => x.name != origin && x.`type` == "opphavsmann")
      Copyright(license, origin, authorsExcludingOrigin)
    }

    def toDomainArticle(newArticle: api.NewArticle): Article = {
      Article(
        id=None,
        revision=None,
        title=newArticle.title.map(toDomainTitle),
        content=newArticle.content.map(toDomainContent),
        copyright=toDomainCopyright(newArticle.copyright),
        tags=newArticle.tags.map(toDomainTag),
        requiredLibraries=newArticle.requiredLibraries.getOrElse(Seq()).map(toDomainRequiredLibraries),
        visualElement=newArticle.visualElement.getOrElse(Seq()).map(toDomainVisualElement),
        introduction=newArticle.introduction.getOrElse(Seq()).map(toDomainIntroduction),
        metaDescription=newArticle.metaDescription.getOrElse(Seq()).map(toDomainMetaDescription),
        metaImageId=newArticle.metaImageId,
        created=clock.now(),
        updated=clock.now(),
        updatedBy=authUser.id(),
        newArticle.articleType
      )
    }

    def toDomainArticle(newArticle: api.NewArticleV2): Article = {
      val domainTitle = Seq(ArticleTitle(newArticle.title, newArticle.language))
      val domainContent = Seq(ArticleContent(
        removeUnknownEmbedTagAttributes(newArticle.content),
        newArticle.footNotes.map(toDomainFootNotes),
        newArticle.language)
      )

      Article(
        id=None,
        revision=None,
        title=domainTitle,
        content=domainContent,
        copyright=toDomainCopyright(newArticle.copyright),
        tags=toDomainTagV2(newArticle.tags, newArticle.language),
        requiredLibraries=newArticle.requiredLibraries.getOrElse(Seq()).map(toDomainRequiredLibraries),
        visualElement=toDomainVisualElementV2(newArticle.visualElement, newArticle.language),
        introduction=toDomainIntroductionV2(newArticle.introduction, newArticle.language),
        metaDescription=toDomainMetaDescriptionV2(newArticle.metaDescription, newArticle.language),
        metaImageId=newArticle.metaImageId,
        created=clock.now(),
        updated=clock.now(),
        updatedBy=authUser.id(),
        newArticle.articleType
      )
    }

    def toDomainTitle(articleTitle: api.ArticleTitle): ArticleTitle = {
      ArticleTitle(articleTitle.title, articleTitle.language)
    }

    def toDomainContent(articleContent: api.ArticleContent): ArticleContent = {
      ArticleContent(removeUnknownEmbedTagAttributes(articleContent.content), articleContent.footNotes.map(toDomainFootNotes), articleContent.language)
    }

    def toDomainTag(tag: api.ArticleTag): ArticleTag = {
      ArticleTag(tag.tags, tag.language)
    }

    def toDomainTagV2(tag: Seq[String], language: String): Seq[ArticleTag] = {
      if (tag.isEmpty) {
        Seq.empty[ArticleTag]
      } else {
        Seq(ArticleTag(tag, language))
      }
    }

    def toDomainVisualElement(visual: api.VisualElement): VisualElement = {
      VisualElement(removeUnknownEmbedTagAttributes(visual.visualElement), visual.language)
    }

    def toDomainVisualElementV2(visual: Option[String], language: String): Seq[VisualElement] = {
      if (visual.isEmpty) {
        Seq.empty[VisualElement]
      } else {
        Seq(VisualElement(removeUnknownEmbedTagAttributes(visual.getOrElse("")), language))
      }
    }

    def toDomainIntroduction(intro: api.ArticleIntroduction): ArticleIntroduction = {
      ArticleIntroduction(intro.introduction, intro.language)
    }

    def toDomainIntroductionV2(intro: Option[String], language: String): Seq[ArticleIntroduction] = {
      if (intro.isEmpty) {
        Seq.empty[ArticleIntroduction]
      } else {
        Seq(ArticleIntroduction(intro.getOrElse(""), language))
      }
    }

    def toDomainMetaDescription(meta: api.ArticleMetaDescription): ArticleMetaDescription = {
      ArticleMetaDescription(meta.metaDescription, meta.language)
    }

    def toDomainMetaDescriptionV2(meta: Option[String], language: String): Seq[ArticleMetaDescription]= {
      if (meta.isEmpty) {
        Seq.empty[ArticleMetaDescription]
      } else {
        Seq(ArticleMetaDescription(meta.getOrElse(""), language))
      }
    }

    def toDomainFootNotes(footNotes: Map[String, api.FootNoteItem]): Map[String, FootNoteItem] = {
      footNotes map { case (key, value) => key -> toDomainFootNote(value) }
    }

    def toDomainFootNote(footNote: api.FootNoteItem): FootNoteItem = {
      FootNoteItem(footNote.title, footNote.`type`, footNote.year, footNote.edition, footNote.publisher, footNote.authors)
    }

    def toDomainCopyright(copyright: api.Copyright): Copyright = {
      Copyright(copyright.license.license, copyright.origin, copyright.authors.map(toDomainAuthor))
    }

    def toDomainAuthor(author: api.Author): Author = {
      Author(author.`type`, author.name)
    }

    def toDomainRequiredLibraries(requiredLibs: api.RequiredLibrary): RequiredLibrary = {
      RequiredLibrary(requiredLibs.mediaType, requiredLibs.name, requiredLibs.url)
    }

    private def getLinkToOldNdla(id: Long): Option[String] = {
      articleRepository.getExternalIdFromId(id).map(createLinkToOldNdla)
    }

    private def removeUnknownEmbedTagAttributes(html: String): String = {
      val document = stringToJsoupDocument(html)
      document.select("embed").asScala.map(el => {
        ResourceType.valueOf(el.attr(Attributes.DataResource.toString))
          .map(EmbedTag.attributesForResourceType)
          .map(knownAttributes => HTMLCleaner.removeIllegalAttributes(el, knownAttributes.all.map(_.toString)))
      })

      jsoupDocumentToString(document)
    }

    def toApiArticle(article: Article): api.Article = {
      api.Article(
        article.id.get.toString,
        article.id.flatMap(getLinkToOldNdla),
        article.revision.get,
        article.title.map(toApiArticleTitle),
        article.content.map(toApiArticleContent),
        toApiCopyright(article.copyright),
        article.tags.map(toApiArticleTag),
        article.requiredLibraries.map(toApiRequiredLibrary),
        article.visualElement.map(toApiVisualElement),
        article.introduction.map(toApiArticleIntroduction),
        article.metaDescription.map(toApiArticleMetaDescription),
        article.created,
        article.updated,
        article.updatedBy,
        article.articleType
      )
    }

    def toApiArticleV2(article: Article, language: String): Option[api.ArticleV2] = {
      val supportedLanguages = getSupportedLanguages(
        Seq(article.title, article.visualElement, article.introduction, article.metaDescription, article.tags, article.content)
      )

      if (supportedLanguages.isEmpty || (!supportedLanguages.contains(language) && language != AllLanguages)) return None
      val searchLanguage = getSearchLanguage(language, supportedLanguages)

      val meta = findByLanguageOrBestEffort(article.metaDescription, language).map(toApiArticleMetaDescription).getOrElse(api.ArticleMetaDescription("", DefaultLanguage))
      val tags = findByLanguageOrBestEffort(article.tags, language).map(toApiArticleTag).getOrElse(api.ArticleTag(Seq(), DefaultLanguage))
      val title = findByLanguageOrBestEffort(article.title, language).map(toApiArticleTitle).getOrElse(api.ArticleTitle("", DefaultLanguage))
      val introduction = findByLanguageOrBestEffort(article.introduction, language).map(toApiArticleIntroduction)
      val visualElement = findByLanguageOrBestEffort(article.visualElement, language).map(toApiVisualElement)
      val articleContent = findByLanguageOrBestEffort(article.content, language).map(toApiArticleContent).getOrElse(api.ArticleContent("", None, DefaultLanguage))


      Some(api.ArticleV2(
        article.id.get,
        article.id.flatMap(getLinkToOldNdla),
        article.revision.get,
        title,
        articleContent,
        toApiCopyright(article.copyright),
        tags,
        article.requiredLibraries.map(toApiRequiredLibrary),
        visualElement,
        introduction,
        meta,
        article.created,
        article.updated,
        article.updatedBy,
        article.articleType,
        supportedLanguages
      ))
    }

    def toApiArticleTitle(title: ArticleTitle): api.ArticleTitle = {
      api.ArticleTitle(title.title, title.language)
    }

    def toApiArticleContent(content: ArticleContent): api.ArticleContent = {
      api.ArticleContent(
        content.content,
        content.footNotes.map(_ map {case (key, value) => key -> toApiFootNoteItem(value)}),
        content.language)
    }

    def toApiArticleContentV2(content: ArticleContent): api.ArticleContentV2 = {
      api.ArticleContentV2(
        content.content,
        content.footNotes.map(_ map { case (key, value) => key -> toApiFootNoteItem(value)})
      )
    }

    def toApiFootNoteItem(footNote: FootNoteItem): api.FootNoteItem = {
      api.FootNoteItem(footNote.title, footNote.`type`, footNote.year, footNote.edition, footNote.publisher, footNote.authors)
    }

    def toApiCopyright(copyright: Copyright): api.Copyright = {
      api.Copyright(
        toApiLicense(copyright.license),
        copyright.origin,
        copyright.authors.map(toApiAuthor)
      )
    }

    def toApiLicense(shortLicense: String): api.License = {
      getLicense(shortLicense) match {
        case Some(l) => api.License(l.license, Option(l.description), l.url)
        case None => api.License("unknown", None, None)
      }
    }

    def toApiAuthor(author: Author): api.Author = {
      api.Author(author.`type`, author.name)
    }

    def toApiArticleTag(tag: ArticleTag): api.ArticleTag = {
      api.ArticleTag(tag.tags, tag.language)
    }

    def toApiRequiredLibrary(required: RequiredLibrary): api.RequiredLibrary = {
      api.RequiredLibrary(required.mediaType, required.name, required.url)
    }

    def toApiVisualElement(visual: VisualElement): api.VisualElement = {
      api.VisualElement(visual.resource, visual.language)
    }

    def toApiArticleIntroduction(intro: ArticleIntroduction): api.ArticleIntroduction = {
      api.ArticleIntroduction(intro.introduction, intro.language)
    }

    def toApiArticleMetaDescription(metaDescription: ArticleMetaDescription): api.ArticleMetaDescription= {
      api.ArticleMetaDescription(metaDescription.content, metaDescription.language)
    }

    def toUpdatedArticle(updatedArticle: api.UpdatedArticleV2, fullArticle: Article): api.UpdatedArticle = {
      val title = updatedArticle.title.map(t => api.ArticleTitle(t, updatedArticle.language)).toSeq
      val content = updatedArticle.content match {
        case Some(updatedContent) => api.ArticleContent(updatedContent, updatedArticle.footNotes, updatedArticle.language)
        case None => fullArticle.content.find(_.language == updatedArticle.language)
          .map(c => api.ArticleContent(c.content, updatedArticle.footNotes, updatedArticle.language))
            .getOrElse(api.ArticleContent("", updatedArticle.footNotes, updatedArticle.language))
      }


      val tags = Seq(api.ArticleTag(updatedArticle.tags, updatedArticle.language))
      val introduction = updatedArticle.introduction.map(i => api.ArticleIntroduction(i, updatedArticle.language)).toSeq
      val meta= updatedArticle.metaDescription.map(m => api.ArticleMetaDescription(m, updatedArticle.language)).toSeq
      val vElement = updatedArticle.visualElement.map(v => api.VisualElement(v, updatedArticle.language)).toSeq
      val reqLibraries = updatedArticle.requiredLibraries

      api.UpdatedArticle(
        title,
        updatedArticle.revision,
        Seq(content),
        tags,
        introduction,
        meta,
        updatedArticle.metaImageId,
        vElement,
        updatedArticle.copyright,
        reqLibraries,
        updatedArticle.articleType
      )
    }

    def createLinkToOldNdla(nodeId: String): String = s"//red.ndla.no/node/$nodeId"

    def toApiConcept(concept: Concept, language: String): api.Concept = {
      val title = findByLanguageOrBestEffort(concept.title, language).map(toApiConceptTitle).getOrElse(api.ConceptTitle("", Language.DefaultLanguage))
      val content = findByLanguageOrBestEffort(concept.content, language).map(toApiConceptContent).getOrElse(api.ConceptContent("", Language.DefaultLanguage))

      api.Concept(
        concept.id.get,
        title,
        content,
        concept.authors.map(toApiAuthor),
        concept.created,
        concept.updated,
        concept.supportedLanguages
      )
    }

    def toApiConceptTitle(title: ConceptTitle): api.ConceptTitle = api.ConceptTitle(title.title, title.language)

    def toApiConceptContent(title: ConceptContent): api.ConceptContent= api.ConceptContent(title.content, title.language)

  }
}
