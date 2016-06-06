package no.ndla.contentapi.integration

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource
import no.ndla.contentapi.model._
import no.ndla.contentapi.service.Tags
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

  class CMData(cmHost: Option[String], cmPort: Option[String],
               cmDatatbase: Option[String], cmUser: Option[String], cmPass: Option[String]) {
    val host = cmHost.getOrElse(throw new RuntimeException("Missing host"))
    val port = cmPort.getOrElse(throw new RuntimeException("Missing host"))
    val database = cmDatatbase.getOrElse(throw new RuntimeException("Missing database"))
    val user = cmUser.getOrElse(throw new RuntimeException("Missing user"))
    val password = cmPass.getOrElse(throw new RuntimeException("Missing password"))

    Class.forName("com.mysql.jdbc.Driver")

    val cmDatasource = new MysqlConnectionPoolDataSource
    cmDatasource.setPassword(password)
    cmDatasource.setUser(user)
    cmDatasource.setUrl(s"jdbc:mysql://$host:$port/$database")

    ConnectionPool.add('cm, new DataSourceConnectionPool(cmDatasource))

    def getNode(nodeId: String): ContentInformation = {
      val (titles, contents) = getNodeMeta(nodeId)
      val authors = getNodeAuthors(nodeId)
      val license = License(license=getNodeCopyrightLicence(nodeId), "", Some(""))
      val copyright = Copyright(license, "", authors)
      val requiredLibraries = List(RequiredLibrary("", "", ""))
      ContentInformation("0", titles, contents, copyright, Tags.forContent(nodeId), requiredLibraries)
    }

    def getNodeMeta(nodeId: String): (List[ContentTitle], List[Content]) = {
      val result = NamedDB('cm) readOnly { implicit session =>
        sql"""
          select nodes.nid as id, nodes.language, v.title, v.body from node n
            left join node nodes on nodes.tnid=n.tnid
            left join node_revisions v on v.vid=nodes.vid
            where n.nid=${nodeId}
          """.stripMargin.map(rs => (rs.string("title"), rs.string("language"), rs.string("body"))).list.apply()
      }
      result.map(x => (ContentTitle(x._1, Some(x._2)), Content(x._3, Some(x._2)))).unzip
    }
    
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

    def getNodeCopyrightLicence(nodeId: String): String = {
      val result = NamedDB('cm) readOnly { implicit session =>
        sql"""
          select cc.license from node n
            left join creativecommons_lite cc on (n.nid = cc.nid)
            where n.nid=${nodeId}
          """.stripMargin.map(rs => rs.string("license")).single.apply()
      }
      result.get
    }

    def getNodeType(nodeId: String): Option[String] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
           select type from node n
             where n.nid=${nodeId}
             """.stripMargin.map(rs => rs.string("type")).single.apply()
      }
    }
  }
}

