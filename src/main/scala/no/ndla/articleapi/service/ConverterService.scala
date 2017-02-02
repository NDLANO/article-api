/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import com.typesafe.scalalogging.LazyLogging

import scala.annotation.tailrec
import no.ndla.articleapi.ArticleApiProperties.maxConvertionRounds
import no.ndla.articleapi.integration.ImageApiClient
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.{api, domain}
import no.ndla.mapping.License.getLicense
import org.joda.time.DateTime

trait ConverterService {
  this: ConverterModules with ExtractConvertStoreContent with ImageApiClient with Clock =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    def toDomainArticle(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (domain.Article, ImportStatus) = {
      val updatedVisitedNodes = importStatus.visitedNodes ++ nodeToConvert.contents.map(_.nid)
      val (convertedContent, converterStatus) = convert(nodeToConvert, maxConvertionRounds, importStatus.copy(visitedNodes = updatedVisitedNodes.distinct))
      val (postProcessed, postProcessStatus) = postProcess(convertedContent, converterStatus)

      (toDomainArticle(postProcessed), postProcessStatus)
    }

    @tailrec private def convert (nodeToConvert: NodeToConvert, maxRoundsLeft: Int, importStatus: ImportStatus): (NodeToConvert, ImportStatus) = {
      if (maxRoundsLeft == 0) {
        val message = "Maximum number of converter rounds reached; Some content might not be converted"
        logger.warn(message)
        return (nodeToConvert, importStatus.copy(messages=importStatus.messages :+ message))
      }

      val (updatedContent, updatedStatus) = executeConverterModules(nodeToConvert, importStatus)

      // If this converting round did not yield any changes to the content, this node is finished (case true)
      // If changes were made during this convertion, we run the converters again (case false)
      updatedContent == nodeToConvert match {
        case true => (updatedContent, updatedStatus)
        case false => convert(updatedContent, maxRoundsLeft - 1, updatedStatus)
      }
    }

    private def postProcess(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (NodeToConvert, ImportStatus) =
      executePostprocessorModules(nodeToConvert, importStatus)


    private def toDomainArticle(nodeToConvert: NodeToConvert): (domain.Article) = {
      val requiredLibraries = nodeToConvert.contents.flatMap(_.requiredLibraries).distinct

      val ingresses = nodeToConvert.contents.flatMap(content => content.asArticleIntroduction)

      domain.Article(None,
        None,
        nodeToConvert.titles,
        nodeToConvert.contents.map(_.asContent),
        toDomainCopyright(nodeToConvert.license, nodeToConvert.authors),
        nodeToConvert.tags,
        requiredLibraries,
        nodeToConvert.visualElements,
        ingresses,
        nodeToConvert.contents.map(_.asArticleMetaDescription),
        None,
        nodeToConvert.created,
        nodeToConvert.updated,
        nodeToConvert.contentType)
    }

    private def toDomainCopyright(license: String, authors: Seq[Author]): Copyright = {
      val origin = authors.find(author => author.`type`.toLowerCase == "opphavsmann").map(_.name).getOrElse("")
      val authorsExcludingOrigin = authors.filterNot(x => x.name != origin && x.`type` == "opphavsmann")
      Copyright(license, origin, authorsExcludingOrigin)
    }

    def toDomainVisualElement(visual: api.VisualElement): domain.VisualElement = {
      domain.VisualElement(visual.content, visual.language)
    }

    def toDomainMetaDescription(meta: api.ArticleMetaDescription): domain.ArticleMetaDescription = {
      domain.ArticleMetaDescription(meta.metaDescription, meta.language)
    }

    def toDomainArticle(newArticle: api.NewArticle): domain.Article = {
      domain.Article(
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
        contentType=newArticle.contentType
      )
    }

    def toDomainArticle(updatedArticle: api.UpdatedArticle): domain.Article = {
      domain.Article(
        id=None,
        revision=Option(updatedArticle.revision),
        title=updatedArticle.title.map(toDomainTitle),
        content=updatedArticle.content.map(toDomainContent),
        copyright=toDomainCopyright(updatedArticle.copyright),
        tags=updatedArticle.tags.map(toDomainTag),
        requiredLibraries=updatedArticle.requiredLibraries.getOrElse(Seq()).map(toDomainRequiredLibraries),
        visualElement=updatedArticle.visualElement.getOrElse(Seq()).map(toDomainVisualElement),
        introduction=updatedArticle.introduction.getOrElse(Seq()).map(toDomainIntroduction),
        metaDescription=updatedArticle.metaDescription.getOrElse(Seq()).map(toDomainMetaDescription),
        metaImageId=updatedArticle.metaImageId,
        created=clock.now(),
        updated=clock.now(),
        contentType=updatedArticle.contentType
      )
    }

    def toDomainTitle(articleTitle: api.ArticleTitle): domain.ArticleTitle = {
      domain.ArticleTitle(articleTitle.title, articleTitle.language)
    }

    def toDomainContent(articleContent: api.ArticleContent): domain.ArticleContent = {
      domain.ArticleContent(articleContent.content, articleContent.footNotes.map(toDomainFootNotes), articleContent.language)
    }

    def toDomainFootNotes(footNotes: Map[String, api.FootNoteItem]): Map[String, domain.FootNoteItem] = {
      footNotes map { case (key, value) => key -> toDomainFootNote(value) }
    }

    def toDomainFootNote(footNote: api.FootNoteItem): domain.FootNoteItem = {
      domain.FootNoteItem(footNote.title, footNote.`type`, footNote.year, footNote.edition, footNote.publisher, footNote.authors)
    }

    def toDomainCopyright(copyright: api.Copyright): domain.Copyright = {
      domain.Copyright(copyright.license.license, copyright.origin, copyright.authors.map(toDomainAuthor))
    }

    def toDomainAuthor(author: api.Author): domain.Author = {
      domain.Author(author.`type`, author.name)
    }

    def toDomainTag(tag: api.ArticleTag): domain.ArticleTag = {
      domain.ArticleTag(tag.tags, tag.language)
    }

    def toDomainRequiredLibraries(requiredLibs: api.RequiredLibrary): domain.RequiredLibrary = {
      domain.RequiredLibrary(requiredLibs.mediaType, requiredLibs.name, requiredLibs.url)
    }

    def toDomainIntroduction(intro: api.ArticleIntroduction): domain.ArticleIntroduction = {
      domain.ArticleIntroduction(intro.introduction, intro.language)
    }

    def toApiArticle(article: domain.Article): api.Article = {
      api.Article(
        article.id.get.toString,
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
        article.contentType
      )
    }

    def toApiArticleTitle(title: domain.ArticleTitle): api.ArticleTitle = {
      api.ArticleTitle(title.title, title.language)
    }

    def toApiArticleContent(content: domain.ArticleContent): api.ArticleContent = {
      api.ArticleContent(
        content.content,
        content.footNotes.map(_ map {case (key, value) => key -> toApiFootNoteItem(value)}),
        content.language)
    }

    def toApiFootNoteItem(footNote: domain.FootNoteItem): api.FootNoteItem = {
      api.FootNoteItem(footNote.title, footNote.`type`, footNote.year, footNote.edition, footNote.publisher, footNote.authors)
    }

    def toApiCopyright(copyright: domain.Copyright): api.Copyright = {
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

    def toApiAuthor(author: domain.Author): api.Author = {
      api.Author(author.`type`, author.name)
    }

    def toApiArticleTag(tag: domain.ArticleTag): api.ArticleTag = {
      api.ArticleTag(tag.tags, tag.language)
    }

    def toApiRequiredLibrary(required: domain.RequiredLibrary): api.RequiredLibrary = {
      api.RequiredLibrary(required.mediaType, required.name, required.url)
    }

    def toApiVisualElement(visual: domain.VisualElement): api.VisualElement = {
      api.VisualElement(visual.resource, visual.language)
    }

    def toApiArticleIntroduction(intro: domain.ArticleIntroduction): api.ArticleIntroduction = {
      api.ArticleIntroduction(intro.introduction, intro.language)
    }

    def toApiArticleMetaDescription(metaDescription: domain.ArticleMetaDescription): api.ArticleMetaDescription= {
      api.ArticleMetaDescription(metaDescription.content, metaDescription.language)
    }

  }
}
