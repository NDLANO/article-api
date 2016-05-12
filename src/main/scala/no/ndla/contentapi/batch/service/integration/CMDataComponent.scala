package no.ndla.contentapi.batch.service.integration

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource
import no.ndla.contentapi.batch.Node
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool, NamedDB, _}

/**
  * Forfatter og body for en node id
  * select n.nid as ID, n.type as Type, td.name as author_type, person.title AS Author, v.body as content, v.teaser from node n
  * left join ndla_authors na on na.nid=n.nid
  * LEFT JOIN term_data td ON na.tid = td.tid
  * left join node person on person.nid=na.person_nid
  * left join node_revisions v on v.vid=n.vid
  * where n.nid=159967
  */

trait CMDataComponent {
  val cmData: CMData

  class CMData(cmHost: Option[String], cmPort: Option[String], cmDatatbase: Option[String], cmUser: Option[String], cmPass: Option[String]) {
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

    def getNode(nodeId: String): Node = {
      Node(content=getNodeContent(nodeId).get)
    }

    def getNodeContent(nodeId: String): Option[String] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
           |select n.nid as ID, v.body as content from node n
             |left join node_revisions v on v.vid=n.vid
             |where n.nid=${nodeId}
             |limit 2;
             """.stripMargin.map(rs => rs.string("content")).single.apply()
      }
    }
  }
}
