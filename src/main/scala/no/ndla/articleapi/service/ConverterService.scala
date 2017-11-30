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
import no.ndla.articleapi.ArticleApiProperties._
import no.ndla.articleapi.auth.User
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.integration.{DraftApiClient, ImageApiClient}
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.ArticleSummaryV2
import no.ndla.articleapi.model.domain.Language._
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.mapping.License.getLicense
import no.ndla.network.ApplicationUrl
import no.ndla.validation.{HtmlTagRules, EmbedTagRules, ResourceType, TagAttributes}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: ConverterModules with ExtractConvertStoreContent with ImageApiClient with Clock with ArticleRepository with DraftApiClient with User =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {

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
      val introductions = getEntrySetSeq(hit, "introduction").map(entr => ArticleIntroduction(entr.getValue.getAsString, entr.getKey))
      val visualElements = getEntrySetSeq(hit, "visualElement").map(entr => VisualElement(entr.getValue.getAsString, entr.getKey))

      val supportedLanguages = getSupportedLanguages(Seq(titles, visualElements, introductions))

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
      val nodeIdsToImport = nodeToConvert.contents.map(_.nid).toSet

      convert(nodeToConvert, maxConvertionRounds, importStatus.addVisitedNodes(nodeIdsToImport))
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
        return Success((nodeToConvert, importStatus.copy(messages = importStatus.messages :+ message)))
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
      val license = Option(convertedNode.license).filter(_.nonEmpty)

      Concept(
        None,
        convertedNode.titles.map(title => ConceptTitle(title.title, title.language)),
        convertedNode.contents.map(content => ConceptContent(content.content, content.language)),
        license.map(l => toDomainCopyright(l, convertedNode.authors)),
        convertedNode.created,
        convertedNode.updated
      )
    }

    private def toNewAuthorType(author: Author): Author = {
      val creatorMap = (oldCreatorTypes zip creatorTypes).toMap.withDefaultValue(None)
      val processorMap = (oldProcessorTypes zip processorTypes).toMap.withDefaultValue(None)
      val rightsholderMap = (oldRightsholderTypes zip rightsholderTypes).toMap.withDefaultValue(None)

      (creatorMap(author.`type`.toLowerCase), processorMap(author.`type`.toLowerCase), rightsholderMap(author.`type`.toLowerCase)) match {
        case (t: String, _, _) => Author(t.capitalize, author.name)
        case (_, t: String, _) => Author(t.capitalize, author.name)
        case (_, _, t: String) => Author(t.capitalize, author.name)
        case (_, _, _) => Author(author.`type`, author.name)
      }
    }

    private def toDomainCopyright(license: String, authors: Seq[Author]): Copyright = {
      val origin = authors.find(author => author.`type`.toLowerCase == "opphavsmann").map(_.name).getOrElse("")


      val authorsExcludingOrigin = authors.filterNot(x => x.name != origin && x.`type` == "opphavsmann")
      val creators = authorsExcludingOrigin.map(toNewAuthorType).filter(a => creatorTypes.contains(a.`type`.toLowerCase))
      // Filters out processor authors with type `editorial` during import process since /`editorial` exists both in processors and creators.
      val processors = authorsExcludingOrigin.map(toNewAuthorType).filter(a => processorTypes.contains(a.`type`.toLowerCase)).filterNot(a => a.`type`.toLowerCase == "editorial")
      val rightsholders = authorsExcludingOrigin.map(toNewAuthorType).filter(a => rightsholderTypes.contains(a.`type`.toLowerCase))
      Copyright(license, origin, creators, processors, rightsholders, None, None, None)
    }

    def toDomainArticle(newArticle: api.NewArticleV2): Article = {
      val domainTitle = Seq(ArticleTitle(newArticle.title, newArticle.language))
      val domainContent = Seq(ArticleContent(
        removeUnknownEmbedTagAttributes(newArticle.content),
        newArticle.language)
      )

      Article(
        id = None,
        revision = None,
        title = domainTitle,
        content = domainContent,
        copyright = toDomainCopyright(newArticle.copyright),
        tags = toDomainTagV2(newArticle.tags, newArticle.language),
        requiredLibraries = newArticle.requiredLibraries.getOrElse(Seq()).map(toDomainRequiredLibraries),
        visualElement = toDomainVisualElementV2(newArticle.visualElement, newArticle.language),
        introduction = toDomainIntroductionV2(newArticle.introduction, newArticle.language),
        metaDescription = toDomainMetaDescriptionV2(newArticle.metaDescription, newArticle.language),
        metaImageId = newArticle.metaImageId,
        created = clock.now(),
        updated = clock.now(),
        updatedBy = authUser.id(),
        newArticle.articleType
      )
    }

    def withAgreementCopyright(article: Article): Article = {
      val agreementCopyright = article.copyright.agreementId.flatMap(aid =>
        draftApiClient.getAgreementCopyright(aid).map(toDomainCopyright)
      ).getOrElse(article.copyright)

      article.copy(copyright = article.copyright.copy(
        license = agreementCopyright.license,
        creators = agreementCopyright.creators,
        rightsholders = agreementCopyright.rightsholders,
        validFrom = agreementCopyright.validFrom,
        validTo = agreementCopyright.validTo
      ))
    }

    def withAgreementCopyright(copyright: api.Copyright): api.Copyright = {
      val agreementCopyright = copyright.agreementId.flatMap(aid => draftApiClient.getAgreementCopyright(aid)).getOrElse(copyright)
      copyright.copy(
        license = agreementCopyright.license,
        creators = agreementCopyright.creators,
        rightsholders = agreementCopyright.rightsholders,
        validFrom = agreementCopyright.validFrom,
        validTo = agreementCopyright.validTo
      )
    }

    def toDomainTitle(articleTitle: api.ArticleTitle): ArticleTitle = {
      ArticleTitle(articleTitle.title, articleTitle.language)
    }

    def toDomainContent(articleContent: api.ArticleContentV2): ArticleContent = {
      ArticleContent(removeUnknownEmbedTagAttributes(articleContent.content), articleContent.language)
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

    def toDomainMetaDescriptionV2(meta: Option[String], language: String): Seq[ArticleMetaDescription] = {
      if (meta.isEmpty) {
        Seq.empty[ArticleMetaDescription]
      } else {
        Seq(ArticleMetaDescription(meta.getOrElse(""), language))
      }
    }

    def toDomainCopyright(copyright: api.Copyright): Copyright = {
      Copyright(
        copyright.license.license,
        copyright.origin,
        copyright.creators.map(toDomainAuthor),
        copyright.processors.map(toDomainAuthor),
        copyright.rightsholders.map(toDomainAuthor),
        copyright.agreementId,
        copyright.validFrom,
        copyright.validTo)
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
        ResourceType.valueOf(el.attr(TagAttributes.DataResource.toString))
          .map(EmbedTagRules.attributesForResourceType)
          .map(knownAttributes => HtmlTagRules.removeIllegalAttributes(el, knownAttributes.all.map(_.toString)))
      })

      jsoupDocumentToString(document)
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
      val articleContent = findByLanguageOrBestEffort(article.content, language).map(toApiArticleContentV2).getOrElse(api.ArticleContentV2("", DefaultLanguage))
      val metaImage = article.metaImageId.map(toApiMetaImage)


      Some(api.ArticleV2(
        article.id.get,
        article.id.flatMap(getLinkToOldNdla),
        article.revision.get,
        title,
        articleContent,
        withAgreementCopyright(toApiCopyright(article.copyright)),
        tags,
        article.requiredLibraries.map(toApiRequiredLibrary),
        visualElement,
        metaImage,
        introduction,
        meta,
        article.created,
        article.updated,
        article.updatedBy,
        article.articleType,
        supportedLanguages
      ))
    }

    def toApiMetaImage(metaImageId: String): String = {
      s"${externalApiUrls("raw-image")}/$metaImageId"
    }
    def toApiArticleTitle(title: ArticleTitle): api.ArticleTitle = {
      api.ArticleTitle(title.title, title.language)
    }

    def toApiArticleContentV2(content: ArticleContent): api.ArticleContentV2 = {
      api.ArticleContentV2(
        content.content,
        content.language
      )
    }

    def toApiCopyright(copyright: Copyright): api.Copyright = {
      api.Copyright(
        toApiLicense(copyright.license),
        copyright.origin,
        copyright.creators.map(toApiAuthor),
        copyright.processors.map(toApiAuthor),
        copyright.rightsholders.map(toApiAuthor),
        copyright.agreementId,
        copyright.validFrom,
        copyright.validTo
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

    def toApiArticleMetaDescription(metaDescription: ArticleMetaDescription): api.ArticleMetaDescription = {
      api.ArticleMetaDescription(metaDescription.content, metaDescription.language)
    }

    def createLinkToOldNdla(nodeId: String): String = s"//red.ndla.no/node/$nodeId"

    def toApiConcept(concept: Concept, language: String): api.Concept = {
      val title = findByLanguageOrBestEffort(concept.title, language).map(toApiConceptTitle).getOrElse(api.ConceptTitle("", Language.DefaultLanguage))
      val content = findByLanguageOrBestEffort(concept.content, language).map(toApiConceptContent).getOrElse(api.ConceptContent("", Language.DefaultLanguage))

      api.Concept(
        concept.id.get,
        title,
        content,
        concept.copyright.map(toApiCopyright),
        concept.created,
        concept.updated,
        concept.supportedLanguages
      )
    }

    def toApiConceptTitle(title: ConceptTitle): api.ConceptTitle = api.ConceptTitle(title.title, title.language)

    def toApiConceptContent(title: ConceptContent): api.ConceptContent = api.ConceptContent(title.content, title.language)

  }

}
