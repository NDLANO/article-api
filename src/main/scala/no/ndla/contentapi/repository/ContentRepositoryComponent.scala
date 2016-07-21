package no.ndla.contentapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.ContentApiProperties
import no.ndla.contentapi.integration.DataSourceComponent
import no.ndla.contentapi.model.{ContentInformation, ContentSummary}
import org.postgresql.util.PGobject
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool, _}
import no.ndla.network.ApplicationUrl

trait ContentRepositoryComponent {
  this: DataSourceComponent =>
  val contentRepository: ContentRepository

  class ContentRepository extends LazyLogging {

    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

    def insert(contentInformation: ContentInformation, externalId: String)(implicit session: DBSession = AutoSession): Long = {
      import org.json4s.native.Serialization.write
      implicit val formats = org.json4s.DefaultFormats

      val json = write(contentInformation)
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      val contentId: Long = sql"insert into contentdata(external_id, document) values(${externalId}, ${dataObject})".updateAndReturnGeneratedKey().apply

      logger.info(s"Inserted node ${externalId}: $contentId")
      contentId
    }

    def update(contentInformation: ContentInformation, externalId: String)(implicit session: DBSession = AutoSession): Long = {
      import org.json4s.native.Serialization.write
      implicit val formats = org.json4s.DefaultFormats

      val json = write(contentInformation)
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      val contentId: Long = sql"update contentdata set document = ${dataObject} where external_id = ${externalId}".updateAndReturnGeneratedKey().apply

      logger.info(s"Updated node ${externalId}: $contentId")
      contentId
    }

    def getIdFromExternalId(externalId: String)(implicit session: DBSession = AutoSession): Option[Long] =
      sql"select id from contentdata where external_id=${externalId}".map(rs => rs.long("id")).single.apply()

    def minMaxId: (Long, Long) = {
      DB readOnly { implicit session =>
        sql"select min(id) as mi, max(id) as ma from contentdata;".map(rs => {
          (rs.long("mi"), rs.long("ma"))
        }).single().apply() match {
          case Some(minmax) => minmax
          case None => (0L, 0L)
        }
      }
    }

    def applyToAll(func: (List[ContentInformation]) => Unit): Unit = {
      val (minId, maxId) = minMaxId
      val groupRanges = Seq.range(minId, maxId + 1).grouped(ContentApiProperties.IndexBulkSize).map(group => (group.head, group.last))

      DB readOnly { implicit session =>
        groupRanges.foreach(range => {
          func(
            sql"select id,document from contentdata where id between ${range._1} and ${range._2}".map(rs => {
              asContentInformation(rs.long("id").toString, rs.string("document"))
            }).toList.apply
          )
        })
      }
    }

    def all: List[ContentInformation] = {
      DB readOnly { implicit session =>
        sql"select id, document from contentdata".map(rs => asContentInformation(rs.string("id"), rs.string("document"))).list().apply()
      }
    }

    def withId(contentId: String): Option[ContentInformation] = {
      DB readOnly { implicit session =>
        sql"select document from contentdata where id = ${contentId.toInt}".map(rs => rs.string("document")).single.apply match {
          case Some(json) => Option(asContentInformation(contentId, json))
          case None => None
        }
      }
    }

    def withExternalId(externalId: String)(implicit session: DBSession = AutoSession): Option[ContentSummary] =
        sql"select id, document from contentdata where external_id=$externalId".map(rs => asContentSummary(rs.long("id"), rs.string("document"))).single.apply()

    def exists(externalId: String): Boolean = {
      DB readOnly { implicit session =>
        sql"select exists(select 1 from contentdata where external_id=${externalId})".map(rs => (rs.boolean(1))).single.apply match {
          case Some(t) => t
          case None => false
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
}
