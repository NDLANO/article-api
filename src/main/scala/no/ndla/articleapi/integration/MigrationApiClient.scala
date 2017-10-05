/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.integration

import java.net.URL
import java.util.Date

import no.ndla.articleapi.ArticleApiProperties.{MigrationHost, MigrationPassword, MigrationUser, Environment}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.service.TagsService
import no.ndla.network.NdlaClient

import scala.util.Try
import scalaj.http.Http
import com.netaporter.uri.dsl._

trait MigrationApiClient {
  this: NdlaClient with TagsService =>
  val migrationApiClient: MigrationApiClient

  class MigrationApiClient {
    val DBSource = if (Environment == "prod") "cm" else "red"
    private val ContentMigrationBaseEndpoint = s"$MigrationHost/contents"
    private val ContentDataEndpoint = s"$ContentMigrationBaseEndpoint/:node_id" ? (s"db-source" -> s"$DBSource")
    private val ContentTypeEndpoint = s"$ContentMigrationBaseEndpoint/type/:node_id" ? (s"db-source" -> s"$DBSource")
    private val ContentEmbedEndpoint = s"$ContentMigrationBaseEndpoint/embedmeta/:node_id" ? (s"db-source" -> s"$DBSource")
    private val ContentAudioEndpoint = s"$ContentMigrationBaseEndpoint/audiometa/:node_id" ? (s"db-source" -> s"$DBSource")
    private val ContentFileEndpoint = s"$ContentMigrationBaseEndpoint/filemeta/:node_id" ? (s"db-source" -> s"$DBSource")
    private val ContentGeneralEndpoint = s"$ContentMigrationBaseEndpoint/generalcontent/:node_id" ? (s"db-source" -> s"$DBSource")
    private val ContentBiblioMetaEndpoint = s"$ContentMigrationBaseEndpoint/bibliometa/:node_id" ? (s"db-source" -> s"$DBSource")
    private val ContentSubjectMetaEndpoint = s"$ContentMigrationBaseEndpoint/subjectfornode/:node_id" ? (s"db-source" -> s"$DBSource")

    def getContentNodeData(nodeId: String): Try[MigrationMainNodeImport] =
      get[MigrationMainNodeImport](ContentDataEndpoint, nodeId)

    private def get[A](endpointUrl: String, nodeId: String)(implicit mf: Manifest[A]): Try[A] = {
      ndlaClient.fetchWithBasicAuth[A](
        Http(endpointUrl.replace(":node_id", nodeId)),
        MigrationUser, MigrationPassword)
    }

    def getContentType(nodeId: String): Try[MigrationNodeType] =
      get[MigrationNodeType](ContentTypeEndpoint, nodeId)

    def getNodeEmbedData(nodeId: String): Try[MigrationEmbedMeta] =
      get[MigrationEmbedMeta](ContentEmbedEndpoint, nodeId)

    def getFilMeta(nodeId: String): Try[MigrationContentFileMeta] =
      get[MigrationContentFileMeta](ContentFileEndpoint, nodeId)

    def getNodeGeneralContent(nodeId: String): Try[Seq[MigrationNodeGeneralContent]] =
      get[Seq[MigrationNodeGeneralContent]](ContentGeneralEndpoint, nodeId)

    def getBiblioMeta(nodeId: String): Try[MigrationContentBiblioMeta] =
      get[MigrationContentBiblioMeta](ContentBiblioMetaEndpoint, nodeId)

    def getSubjectForNode(nodeId: String): Try[Seq[MigrationSubjectMeta]] =
      get[Seq[MigrationSubjectMeta]](ContentSubjectMetaEndpoint, nodeId).map(_.distinct)

  }

}

case class MigrationMainNodeImport(titles: Seq[MigrationContentTitle], ingresses: Seq[MigrationIngress], contents: Seq[MigrationContent],
  authors: Seq[MigrationContentAuthor], license: Option[String], nodeType: Option[String],
  pageTitles: Seq[MigrationPageTitle], visualElements: Seq[MigrationVisualElement], relatedContents: Seq[MigrationRelatedContents],
  editorialKeywords: Seq[MigrationEditorialKeywords], learningResourceType: Seq[MigrationLearningResourceType],
  difficulty: Seq[MigrationDifficulty], contentType: Seq[MigrationContentType], innholdAndFag: Seq[MigrationInnholdsKategoriAndFag],
  fagressurs: Seq[MigrationFagressurs], emneartikkelData: Seq[MigrationEmneArtikkelData]) {

  def asNodeToConvert(nodeId  : String, tags: List[ArticleTag]): NodeToConvert = {
    val articleType = nodeType.map(nType => if (nType == "emneartikkel") ArticleType.TopicArticle else ArticleType.Standard)

    NodeToConvert(
      titles.map(x => x.asContentTitle),
      asLanguageContents,
      license.getOrElse(""),
      authors.flatMap(x => x.asAuthor),
      tags,
      nodeType.getOrElse("unknown"),
      contentType.headOption.map(_.`type`).getOrElse("unknown"),
      contents.minBy(_.created).created,
      contents.maxBy(_.changed).changed,
      articleType.getOrElse(ArticleType.Standard)
    )
  }

  def asLanguageContents: Seq[LanguageContent] = {
    contents.map(content => {
      LanguageContent(content.nid,
        content.tnid,
        content.content,
        getMetaDescription(content),
        Language.languageOrUnknown(content.language),
        visualElements.find(_.language == content.language).map(_.element),
        nodeType.getOrElse("unknown"),
        contentType.headOption.map(_.`type`.toLowerCase),
        titles.find(_.language == content.language).map(_.title),
        ingress = getIngress(content.language))
    })
  }

  private def getIngress(language: Option[String]): Option[LanguageIngress] = {
    getEmneArtikkel(language) match {
      case Some(data) => Option(LanguageIngress(data.ingress, None))
      case None =>
        ingresses.find(ingress => ingress.language == language && ingress.ingressVisPaaSiden == 1)
          .map(ingress => LanguageIngress(ingress.content.getOrElse(""), ingress.imageNid))
    }
  }

  private def getMetaDescription(content: MigrationContent): String = {
    getEmneArtikkel(content.language) match {
      case Some(data) => data.metaDescription
      case None => content.metaDescription
    }
  }

  private def getEmneArtikkel(language   : Option[String]) = emneartikkelData.find(_.language == language)
}

case class MigrationNodeGeneralContent(nid: String, tnid: String, title: String, content: String, language: String) {
  def asNodeGeneralContent: NodeGeneralContent = NodeGeneralContent(nid, tnid, title, content, language)
}

case class MigrationContentAuthor(`type`: Option[String], name: Option[String]) {
  def asAuthor: Option[Author] = {
    (`type`, name) match {
      case (None, None) => None
      case (authorType, authorName) => Some(Author(authorType.getOrElse(""), authorName.getOrElse("")))
    }
  }
}

case class MigrationContentTitle(title: String, language: Option[String]) {
  def asContentTitle: ArticleTitle = ArticleTitle(title, Language.languageOrUnknown(language))
}

case class MigrationIngress(nid: String, content: Option[String], imageNid: Option[String], ingressVisPaaSiden: Int, language: Option[String])

case class MigrationContent(nid: String, tnid: String, content: String, metaDescription: String, language: Option[String], created: Date, changed: Date)

case class MigrationNodeType(nodeType: String)

case class MigrationContentBiblioMeta(biblio: MigrationBiblio, authors: Seq[MigrationBiblioAuthor]) {
  def asBiblioMeta: BiblioMeta = BiblioMeta(biblio.asBiblio, authors.map(x => x.asBiblioAuthor))
}

case class MigrationBiblio(title: String, bibType: String, year: String, edition: String, publisher: String) {
  def asBiblio: Biblio = Biblio(title, bibType, year, edition, publisher)
}

case class MigrationBiblioAuthor(name: String, lastname: String, firstname: String) {
  def asBiblioAuthor: BiblioAuthor = BiblioAuthor(name, lastname, firstname)
}

case class MigrationContentFileMeta(nid: String, tnid: String, title: String, fileName: String, url: String, mimeType: String, fileSize: String) {
  def asContentFilMeta: ContentFilMeta = ContentFilMeta(nid, tnid, title, fileName, new URL(url), mimeType, fileSize)
}

case class MigrationEmbedMeta(url: Option[String], embedCode: Option[String])

case class MigrationPageTitle(title: String, `type`: String, language: Option[String])

case class MigrationVisualElement(element: String, `type`: String, language: Option[String])

case class MigrationRelatedContents(related: Seq[MigrationRelatedContent], language: Option[String])

case class MigrationRelatedContent(nid: String, title: String, uri: String, fagligRelation: Int)

case class MigrationEditorialKeywords(keywords: Seq[String], language: Option[String])

case class MigrationLearningResourceType(resourceType: String, language: Option[String])

case class MigrationDifficulty(difficulty: String, language: Option[String])

case class MigrationContentType(`type`: String, language: Option[String])

case class MigrationInnholdsKategoriAndFag(innhold: String, fag: String, language: Option[String])

case class MigrationFagressurs(fagressursType: String, velgFagressurs: String, language: Option[String])

case class MigrationSubjectMeta(nid: String, title: String)

case class MigrationEmneArtikkelData(ingress: String, metaDescription: String, language: Option[String])