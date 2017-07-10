/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import com.google.gson.JsonObject
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties.maxConvertionRounds
import no.ndla.articleapi.auth.User
import no.ndla.articleapi.integration.ImageApiClient
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.api
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.mapping.License.getLicense
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.model.api.{ArticleSummary, ArticleSummaryV2}
import no.ndla.articleapi.service.converters.{Attributes, HTMLCleaner, ResourceType}
import no.ndla.network.ApplicationUrl
import io.searchbox.core.{SearchResult => JestSearchResult}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

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
      import scala.collection.JavaConversions._

      ArticleSummary(
        hit.get("id").getAsString,
        hit.get("title").getAsJsonObject.entrySet().to[Seq].map(entr => api.ArticleTitle(entr.getValue.getAsString, Some(entr.getKey))),
        hit.get("visualElement").getAsJsonObject.entrySet().to[Seq].map(entr => api.VisualElement(entr.getValue.getAsString, Some(entr.getKey))),
        hit.get("introduction").getAsJsonObject.entrySet().to[Seq].map(entr => api.ArticleIntroduction(entr.getValue.getAsString, Some(entr.getKey))),
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
      import scala.collection.JavaConversions._

      val titles = hit.get("title").getAsJsonObject.entrySet().to[Seq]
        .map(entr => ArticleTitle(entr.getValue.getAsString, Some(entr.getKey)))
      val searchLanguage = if (language == Language.AllLanguages) Language.DefaultLanguage else language

      /*
      * Find a title that matches the language parameter,
      * the first title if no such language exists,
      * or an empty string if there are no titles.
      * */
      val title = titles
        .find(title => title.language.getOrElse(Language.NoLanguage) == searchLanguage)
        .orElse(titles.headOption).map(_.title)
        .getOrElse("")

      val visualElement = getValueByFieldAndLanguage(hit, "visualElement", searchLanguage)
      val introduction = getValueByFieldAndLanguage(hit, "introduction", searchLanguage)
      val supportedLanguages = titles.map(_.language.getOrElse(Language.NoLanguage))

      ArticleSummaryV2(
        hit.get("id").getAsString,
        title,
        visualElement,
        introduction,
        ApplicationUrl.get + hit.get("id").getAsString,
        hit.get("license").getAsString,
        hit.get("articleType").getAsString,
        supportedLanguages
      )
    }

    def getValueByFieldAndLanguage(hit: JsonObject, fieldPath: String, searchLanguage: String): String = {
      import scala.collection.JavaConversions._

      hit.get(fieldPath).getAsJsonObject.entrySet().to[Seq].find(entr => entr.getKey == searchLanguage) match {
        case Some(element) => element.getValue.getAsString
        case None => ""
      }
    }

    def toDomainArticle(nodeToConvert: NodeToConvert, importStatus: ImportStatus): Try[(Article, ImportStatus)] = {
      val updatedVisitedNodes = importStatus.visitedNodes ++ nodeToConvert.contents.map(_.nid)

      convert(nodeToConvert, maxConvertionRounds, importStatus.copy(visitedNodes = updatedVisitedNodes.distinct))
          .flatMap { case (content, status) => postProcess(content, status) } match {
        case Failure(f) => Failure(f)
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


    private def toDomainArticle(nodeToConvert: NodeToConvert): Article = {
      val requiredLibraries = nodeToConvert.contents.flatMap(_.requiredLibraries).distinct
      val ingresses = nodeToConvert.contents.flatMap(content => content.asArticleIntroduction)
      val visualElements = nodeToConvert.contents.flatMap(_.asVisualElement)

      Article(None,
        None,
        nodeToConvert.titles,
        nodeToConvert.contents.map(_.asContent),
        toDomainCopyright(nodeToConvert.license, nodeToConvert.authors),
        nodeToConvert.tags,
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

    def toDomainArticleV2(newArticle: api.NewArticleV2): Article = {
      val articleLanguage = Some(newArticle.language)

      Article(
        id=None,
        revision=None,
        title=toDomainTitleV2(newArticle.title, articleLanguage),
        content=toDomainContentV2(newArticle.content, articleLanguage),
        copyright=toDomainCopyright(newArticle.copyright),
        tags=toDomainTagV2(newArticle.tags, articleLanguage),
        requiredLibraries=newArticle.requiredLibraries.getOrElse(Seq()).map(toDomainRequiredLibraries),
        visualElement=toDomainVisualElementV2(newArticle.visualElement, articleLanguage),
        introduction=toDomainIntroductionV2(newArticle.introduction, articleLanguage),
        metaDescription=toDomainMetaDescriptionV2(newArticle.metaDescription, articleLanguage),
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

    def toDomainTitleV2(articleTitle: String, language: Option[String]): Seq[ArticleTitle] = {
      Seq(ArticleTitle(articleTitle, language))
    }

    def toDomainContent(articleContent: api.ArticleContent): ArticleContent = {
      ArticleContent(removeUnknownEmbedTagAttributes(articleContent.content), articleContent.footNotes.map(toDomainFootNotes), articleContent.language)
    }

    def toDomainContentV2(articleContent: api.ArticleContent, language: Option[String]): Seq[ArticleContent]= {
      Seq(ArticleContent(removeUnknownEmbedTagAttributes(articleContent.content), articleContent.footNotes.map(toDomainFootNotes), language))
    }

    def toDomainTag(tag: api.ArticleTag): ArticleTag = {
      ArticleTag(tag.tags, tag.language)
    }

    def toDomainTagV2(tag: Seq[String], language: Option[String]): Seq[ArticleTag] = {
      Seq(ArticleTag(tag, language))
    }

    def toDomainVisualElement(visual: api.VisualElement): VisualElement = {
      VisualElement(removeUnknownEmbedTagAttributes(visual.content), visual.language)
    }

    def toDomainVisualElementV2(visual: Option[String], language: Option[String]): Seq[VisualElement] = {
      Seq(VisualElement(removeUnknownEmbedTagAttributes(visual.getOrElse("")), language))
    }

    def toDomainIntroduction(intro: api.ArticleIntroduction): ArticleIntroduction = {
      ArticleIntroduction(intro.introduction, intro.language)
    }

    def toDomainIntroductionV2(intro: Option[String], language: Option[String]): Seq[ArticleIntroduction] = {
      Seq(ArticleIntroduction(intro.getOrElse(""), language))
    }

    def toDomainMetaDescription(meta: api.ArticleMetaDescription): ArticleMetaDescription = {
      ArticleMetaDescription(meta.metaDescription, meta.language)
    }

    def toDomainMetaDescriptionV2(meta: Option[String], language: Option[String]): Seq[ArticleMetaDescription]= {
      Seq(ArticleMetaDescription(meta.getOrElse(""), language))
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
          .map(EmbedTag.requiredAttributesForResourceType)
          .map(requiredAttributes => HTMLCleaner.removeIllegalAttributes(el, requiredAttributes.map(_.toString)))
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

    def toApiArticleV2(article: Article, language: String = Language.DefaultLanguage): api.ArticleV2 = {
      val title = article.title.find(title => title.language.getOrElse("") == language).getOrElse("").toString
      val articleContent = toApiArticleContentV2(article.content.find(content => content.language.getOrElse("") == language).getOrElse(ArticleContent("", None, None)))
      val tags = article.tags.find(tags => tags.language.getOrElse("") == language).getOrElse(ArticleTag(Seq.empty[String], None)).tags
      val visualElement = article.visualElement.find(vElement => vElement.language.getOrElse("") == language).getOrElse(VisualElement("", None)).resource
      val introduction = article.introduction.find(intro => intro.language.getOrElse("") == language).getOrElse(ArticleIntroduction("", None)).introduction
      val meta = article.metaDescription.find(meta => meta.language.getOrElse("") == language).getOrElse(ArticleMetaDescription("", None)).content


      api.ArticleV2(
        article.id.get.toString,
        article.id.flatMap(getLinkToOldNdla),
        article.revision.get,
        language,
        title,
        articleContent.content,
        articleContent.footNotes,
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
        getSupportedLanguages(article)
      )
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

    def createLinkToOldNdla(nodeId: String): String = s"//red.ndla.no/node/$nodeId"

    def getSupportedLanguages(article: Article): Seq[String] = {
      article.title.map(_.language.getOrElse(Language.UnknownLanguage))
        .++:(article.content.map(_.language.getOrElse(Language.UnknownLanguage)))
        .++:(article.tags.map(_.language.getOrElse(Language.UnknownLanguage)))
        .++:(article.visualElement.map(_.language.getOrElse(Language.UnknownLanguage)))
        .++:(article.introduction.map(_.language.getOrElse(Language.UnknownLanguage)))
        .++:(article.metaDescription.map(_.language.getOrElse(Language.UnknownLanguage)))
        .distinct
        .filterNot(_ == Language.UnknownLanguage)
    }

  }
}
