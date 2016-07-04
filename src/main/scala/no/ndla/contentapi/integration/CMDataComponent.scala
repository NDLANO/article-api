package no.ndla.contentapi.integration

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource
import no.ndla.contentapi.model._
import no.ndla.contentapi.service.Tags
import no.ndla.contentapi.ContentApiProperties.ndlaBaseHost
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool, NamedDB, _}

/**
  * Forfatter og body for en node id
  * select n.nid as ID, n.type as Type, td.name as author_type, person.title AS Author, v.body as content, v.teaser from node n
  * left join ndla_authors na on na.nid=n.nid
  * LEFT JOIN term_data td ON na.tid = td.tid
  * left join node person on person.nid=na.person_nid
  * left join node_revisions v on v.vid=n.vid
  * where n.nid=159967
  *
  * Forfattere for en node id
  * select n.nid as ID, td.name as Author_type, person.title from node n
  * left join ndla_authors na on na.nid=n.nid
  * left join term_data td on na.tid=td.tid
  * left join node person on person.nid=na.person_nid
  * where n.nid=148722
  * limit 4
  *
  * Node tittel og content i alle språk
  * select nodes.nid as ID, nodes.language, v.title, v.body from node n
  * left join node nodes on nodes.tnid=n.tnid
  * left join node_revisions v on v.vid=nodes.vid
  * where n.nid=148722
  */

trait CMDataComponent {
  val cmData: CMData

  class CMData(host: String, port: String, database: String, user: String, password: String) {
    Class.forName("com.mysql.jdbc.Driver")

    val cmDatasource = new MysqlConnectionPoolDataSource
    cmDatasource.setPassword(password)
    cmDatasource.setUser(user)
    cmDatasource.setUrl(s"jdbc:mysql://$host:$port/$database")

    ConnectionPool.add('cm, new DataSourceConnectionPool(cmDatasource))

    def getNode(nodeId: String): NodeToConvert = {
      val (contents, titles) = getNodeContent(nodeId)
      val authors = getNodeAuthors(nodeId)
      val license = License(license=getNodeCopyrightLicence(nodeId).getOrElse(""), "", Some(""))
      val copyright = Copyright(license, "", authors)

      NodeToConvert(titles, contents, copyright, Tags.forContent(nodeId))
    }

    def getNodeGeneralContent(nodeId: String): Seq[NodeGeneralContent] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
          select n.nid, n.tnid, n.language, v.title, v.body from node n
            left join node_revisions v on (v.nid = n.nid and v.vid=n.vid)
            where n.nid=${nodeId} or n.tnid=${nodeId}
          """.stripMargin.map(rs => NodeGeneralContent(rs.string("nid"), rs.string("tnid"), rs.string("title"), rs.string("body"), rs.string("language"))).list.apply()
      }
    }

    def getNodeContent(nodeId: String): (Seq[LanguageContent], Seq[ContentTitle]) = {
      val res = getNodeGeneralContent(nodeId)

      res.exists {x => x.isMainNode} match {
        case true => res.map(rs => (rs.asLanguageContent, rs.asContentTitle)).unzip
        case false => {
          if (res.nonEmpty)
            getNodeGeneralContent(res.head.tnid).map(rs => (rs.asLanguageContent, rs.asContentTitle)).unzip
          else
            (Seq(), Seq())
        }
      }
    }

    def getNodeMeta(nodeId: String): (Seq[ContentTitle], Seq[Content]) =
      getNodeGeneralContent(nodeId).map(x => (x.asContentTitle, x.asContent)).unzip

    def getNodeFagstoff(nodeId: String): Seq[ContentFagstoff] =
      getNodeGeneralContent(nodeId).map(x => x.asContentFagstoff)

    def getNodeOppgave(nodeId: String): Seq[ContentOppgave] =
      getNodeGeneralContent(nodeId).map(x => x.asContentOppgave)

    def getNodeAktualitet(nodeId: String): Seq[ContentAktualitet] =
      getNodeGeneralContent(nodeId).map(x => x.asContentAktualitet)

    def getNodeAuthors(nodeId: String): List[Author] = {
      val result = NamedDB('cm) readOnly { implicit session =>
        sql"""
          select td.name as author_type, person.title as author from node n
            left join ndla_authors na on n.vid = na.vid
            left join term_data td on na.tid = td.tid
            left join node person on person.nid = na.person_nid
            where  n.nid=${nodeId}
          """.stripMargin.map(rs => (rs.string("author_type"), rs.string("author"))).list.apply()
      }
      result.map(x => Author(x._1, x._2))
    }

    def getNodeCopyrightLicence(nodeId: String): Option[String] = {
      val result = NamedDB('cm) readOnly { implicit session =>
        sql"""
          select cc.license from node n
            left join creativecommons_lite cc on (n.nid = cc.nid)
            where n.nid=${nodeId}
          """.stripMargin.map(rs => rs.string("license")).single.apply()
      }
      result
    }

    def getNodeType(nodeId: String): Option[String] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
           select type from node n
             where n.nid=${nodeId}
             """.stripMargin.map(rs => rs.string("type")).single.apply()
      }
    }

    def getNodeEmbedData(nodeId: String): Option[String] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
           select n.nid, n.title, url.field_url_url as url, ec.field_embed_code_value as embed_code from node n
           left join content_field_embed_code ec on (ec.nid = n.nid and ec.vid = n.vid)
           left join content_field_url url on (url.nid = n.nid and url.vid = n.vid)
           where n.nid=${nodeId}
          """.stripMargin.map(rs => rs.string("url")).single.apply()
      }
    }

    def getAudioMeta(nodeId: String): Option[AudioMeta] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
           select n.nid, a.title_format as title, a.playtime, a.format, f.filemime, f.filesize, f.filename, f.filepath from node n
           left join audio a on (a.vid=n.vid)
           left join files f on (f.fid=a.fid)
           where n.nid=${nodeId}
          """.stripMargin.map(rs => AudioMeta(
          rs.string("nid"),
          rs.string("title"),
          rs.string("playtime"),
          rs.string("format"),
          rs.string("filemime"),
          rs.string("filesize"),
          rs.string("filename"),
          ndlaBaseHost + rs.string("filepath"))).single.apply()
      }
    }

    def getNodeIngress(nodeId: String): Option[NodeIngress] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
           select n.nid, ing_bilde.field_ingress_bilde_nid as bilde_ing, ing.field_ingress_value as ingress, ing_side.field_ingressvispaasiden_value from node n
           left join content_field_ingress ing on (ing.nid = n.nid and ing.vid = n.vid)
           left join content_field_ingress_bilde ing_bilde on (ing_bilde.nid = n.nid and ing_bilde.vid = n.vid)
           left join content_field_ingressvispaasiden ing_side on (ing_side.nid = n.nid and ing_side.vid = n.vid)
           where n.nid=$nodeId
          """.map(rs => NodeIngress(rs.string("nid"), rs.string("ingress"), rs.stringOpt("bilde_ing"), rs.int("field_ingressvispaasiden_value"))).single.apply()
      }
    }

    def getBiblio(nodeId: String): Option[Biblio] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
          select n.title, t.name as biblio_type, b.biblio_year, b.biblio_edition, b.biblio_publisher from node n
          left join biblio b on (b.nid=n.nid)
          left join biblio_types t on (t.tid=b.biblio_type)
          where b.nid=$nodeId
        """.stripMargin.map(rs => Biblio(
          rs.string("title"),
          rs.string("biblio_type"),
          rs.string("biblio_year"),
          rs.string("biblio_edition"),
          rs.string("biblio_publisher"))).single.apply()
      }
    }

    def getBiblioAuthors(nodeId: String): Seq[BiblioAuthor] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
           select d.name, d.lastname, d.firstname from node n
           left join biblio b on (b.nid=n.nid)
           left join biblio_contributor cont on (cont.nid=b.nid)
           left join biblio_contributor_data d on (d.cid=cont.cid)
           where b.nid=$nodeId
        """.stripMargin.map(rs => BiblioAuthor(rs.string("name"), rs.string("lastname"), rs.string("firstname"))).list.apply()
      }
    }
  }
}
case class NodeGeneralContent(nid: String, tnid: String, title: String, content: String, language: String) {
  def isMainNode = (nid == tnid || tnid == "0")
  def isTranslation = !isMainNode

  def asContentTitle = ContentTitle(title, Some(language))
  def asContent = Content(content, Map(), Some(language))
  def asContentFagstoff = ContentFagstoff(nid, tnid, title, content, language)
  def asContentOppgave =  ContentOppgave(nid, tnid, title, content, language)
  def asContentAktualitet = ContentAktualitet(nid, tnid, title, content, language)
  def asLanguageContent = LanguageContent(nid, tnid, content, Some(language))
}

case class NodeToConvert(titles: Seq[ContentTitle], contents: Seq[LanguageContent], copyright: Copyright, tags: Seq[ContentTag]) {
  def asContentInformation: ContentInformation = {
    val requiredLibraries = contents.flatMap(_.requiredLibraries).distinct
    ContentInformation("0", titles, contents.map(_.asContent), copyright, tags, requiredLibraries)
  }
}

case class ContentFagstoff(nid: String, tnid: String, title: String, fagstoff: String, language: String) {
  def isMainNode = (nid == tnid || tnid == "0")
  def isTranslation = !isMainNode
}

case class ContentOppgave(nid: String, tnid: String, title: String, content: String, language: String) {
  def isMainNode = (nid == tnid || tnid == "0")
  def isTranslation = !isMainNode
}

case class ContentAktualitet(nid: String, tnid: String, title: String, aktualitet: String, language: String) {
  def isMainNode = (nid == tnid || tnid == "0")
  def isTranslation = !isMainNode
}

case class AudioMeta(nodeId: String, title: String, playTime: String, format: String, mimetype: String, fileSize: String, filename: String, url: String)

case class NodeIngress(nid: String, content: String, imageNid: Option[String], ingressVisPaaSiden: Int)

case class Biblio(title: String, bibType: String, year: String, edition: String, publisher: String)

case class BiblioAuthor(name: String, lastname: String, firstname: String)
