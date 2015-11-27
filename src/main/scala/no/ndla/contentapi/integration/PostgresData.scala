package no.ndla.contentapi.integration

import javax.sql.DataSource

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.business.ContentData
import no.ndla.contentapi.model.{ContentSummary, ContentInformation}
import no.ndla.contentapi.network.ApplicationUrl
import org.postgresql.util.PGobject
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool, _}

class PostgresData(dataSource: DataSource) extends ContentData with LazyLogging {

  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  override def insert(contentInformation: ContentInformation, externalId: String) = {
    import org.json4s.native.Serialization.write
    implicit val formats = org.json4s.DefaultFormats

    val json = write(contentInformation)
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(json)

    DB localTx {implicit session =>
      sql"insert into contentdata(external_id, document) values(${externalId}, ${dataObject})".update.apply
    }

    logger.info(s"Inserted ${externalId}")
  }

  override def update(contentInformation: ContentInformation, externalId: String) = {
    import org.json4s.native.Serialization.write
    implicit val formats = org.json4s.DefaultFormats

    val json = write(contentInformation)
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(json)

    DB localTx {implicit session =>
      sql"update contentdata set document = ${dataObject} where external_id = ${externalId}".update.apply
    }

    logger.info(s"Updated ${externalId}")
  }

  override def all: List[ContentSummary] = {
    DB readOnly{implicit session =>
      sql"select id, document from contentdata limit 100".map(rs =>  asContentSummary(rs.long("id"), rs.string("document")) ).list().apply()
    }
  }

  override def withId(contentId: String): Option[ContentInformation] = {
    DB readOnly {implicit session =>
      sql"select document from contentdata where id = ${contentId.toInt}".map(rs => rs.string("document")).single.apply match {
        case Some(json) => Option(asContentInformation(contentId, json))
        case None => None
      }
    }
  }

  override def withExternalId(externalId: String): Option[ContentInformation] = {
    DB readOnly {implicit session =>
      sql"select id, document from contentdata where external_id = ${externalId}".map(rs => (rs.long("id"), rs.string("document"))).single.apply match {
        case Some((id, json)) => Option(asContentInformation(id.toString, json))
        case None => None
      }
    }
  }

  def asContentSummary(contentId: Long, json: String): ContentSummary = {
    import org.json4s.native.Serialization.read
    implicit val formats = org.json4s.DefaultFormats

    val meta = read[ContentInformation](json)
    ContentSummary(contentId.toString, meta.titles.head.title, ApplicationUrl.get() + contentId, meta.copyright.license.license)
  }

  def asContentInformation(contentId: String, json: String): ContentInformation = {
    import org.json4s.native.Serialization.read
    implicit val formats = org.json4s.DefaultFormats

    val meta = read[ContentInformation](json)
    ContentInformation(
      contentId,
      meta.titles,
      meta.content,
      meta.copyright,
      meta.tags,
      meta.requiredLibraries)
  }
}
