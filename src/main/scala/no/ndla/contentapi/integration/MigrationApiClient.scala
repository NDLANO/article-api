/*
 * Part of NDLA content_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.contentapi.integration

import java.net.URL

import no.ndla.contentapi.ContentApiProperties
import no.ndla.contentapi.model._
import no.ndla.contentapi.service.Tags
import no.ndla.network.NdlaClient

import scala.util.Try
import scalaj.http.Http

trait MigrationApiClient {
  this: NdlaClient =>

  val migrationApiClient: MigrationApiClient

  class MigrationApiClient {

    private val ContentMigrationBaseEndpoint = s"${ContentApiProperties.MigrationHost}/contents"
    private val ContentDataEndpoint = s"$ContentMigrationBaseEndpoint/:node_id"
    private val ContentTypeEndpoint = s"$ContentMigrationBaseEndpoint/type/:node_id"
    private val ContentEmbedEndpoint = s"$ContentMigrationBaseEndpoint/embedmeta/:node_id"
    private val ContentAudioEndpoint = s"$ContentMigrationBaseEndpoint/audiometa/:node_id"
    private val ContentFileEndpoint =  s"$ContentMigrationBaseEndpoint/filemeta/:node_id"
    private val ContentGeneralEndpoint = s"$ContentMigrationBaseEndpoint/generalcontent/:node_id"
    private val ContentBiblioMetaEndpoint = s"$ContentMigrationBaseEndpoint/bibliometa/:node_id"

    def getContentNodeData(nodeId: String): Try[MigrationMainNodeImport] =
      get[MigrationMainNodeImport](ContentDataEndpoint, nodeId)

    def getContentType(nodeId: String): Try[MigrationNodeType] =
      get[MigrationNodeType](ContentTypeEndpoint, nodeId)

    def getNodeEmbedData(nodeId: String): Try[MigrationEmbedMeta] =
      get[MigrationEmbedMeta](ContentEmbedEndpoint, nodeId)

    def getAudioMeta(nodeId: String): Try[MigrationContentFileMeta] =
      get[MigrationContentFileMeta](ContentAudioEndpoint, nodeId)

    def getFilMeta(nodeId: String): Try[MigrationContentFileMeta] =
      get[MigrationContentFileMeta](ContentFileEndpoint, nodeId)

    def getNodeGeneralContent(nodeId: String): Try[Seq[MigrationNodeGeneralContent]] =
      get[Seq[MigrationNodeGeneralContent]](ContentGeneralEndpoint, nodeId)

    def getBiblioMeta(nodeId: String): Try[MigrationContentBiblioMeta] =
      get[MigrationContentBiblioMeta](ContentBiblioMetaEndpoint, nodeId)

    private def get[A](endpointUrl: String, nodeId: String)(implicit mf: Manifest[A]): Try[A] = {
      ndlaClient.fetch[A](
        Http(endpointUrl.replace(":node_id", nodeId)),
        Some(ContentApiProperties.MigrationUser), Some(ContentApiProperties.MigrationPassword))
    }

  }
}

case class MigrationMainNodeImport(titles: Seq[MigrationContentTitle], ingresses: Seq[MigrationIngress], contents: Seq[MigrationContent],
                                   authors: Seq[MigrationContentAuthor], license: Option[String], nodeType: Option[String]) {
  def asNodeToConvert(nodeId: String): NodeToConvert = NodeToConvert(
    titles.map(x => x.asContentTitle),
    contents.map(x => x.asLanguageContent),
    Copyright(License(license.getOrElse(""), "", None), "", authors.map(x => x.asAuthor)),
    Tags.forContent(nodeId))
}

case class MigrationNodeGeneralContent(nid: String, tnid: String, title: String, content: String, language: String) {
  def asNodeGeneralContent: NodeGeneralContent = NodeGeneralContent(nid, tnid, title, content, language)
}

case class MigrationContentAuthor(`type`: String, name: String) {
  def asAuthor = Author(`type`, name)
}

case class MigrationContentTitle(title: String, language: Option[String]) {
  def asContentTitle: ContentTitle = ContentTitle(title, language)
}

case class MigrationIngress(nid: String, content: String, imageNid: Option[String], ingressVisPaaSiden: Int) {
  def asNodeIngress: NodeIngress = NodeIngress(nid, content, imageNid, ingressVisPaaSiden)
}

case class MigrationContent(nid: String, tnid: String, content: String, language: Option[String]) {
  def asLanguageContent: LanguageContent = LanguageContent(nid, tnid, content, language)
}

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

case class MigrationEmbedMeta(embed: String)

