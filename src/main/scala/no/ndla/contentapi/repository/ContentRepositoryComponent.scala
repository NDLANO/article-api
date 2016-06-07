package no.ndla.contentapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.ContentApiProperties
import no.ndla.contentapi.business.ContentData
import no.ndla.contentapi.integration.DataSourceComponent
import no.ndla.contentapi.model.{ContentInformation, ContentSummary}
import org.postgresql.util.PGobject
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool, _}
import no.ndla.network.ApplicationUrl

trait ContentRepositoryComponent {
  this: DataSourceComponent =>
  val contentRepository: ContentRepository

  class ContentRepository extends ContentData with LazyLogging {

    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

    override def insert(contentInformation: ContentInformation, externalId: String) = {
      import org.json4s.native.Serialization.write
      implicit val formats = org.json4s.DefaultFormats

      val json = write(contentInformation)
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      DB localTx { implicit session =>
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

      DB localTx { implicit session =>
        sql"update contentdata set document = ${dataObject} where external_id = ${externalId}".update.apply
      }

      logger.info(s"Updated ${externalId}")
    }

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

    override def applyToAll(func: (List[ContentInformation]) => Unit): Unit = {
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

    override def all: List[ContentSummary] = {
      DB readOnly { implicit session =>
        sql"select id, document from contentdata limit 100".map(rs => asContentSummary(rs.long("id"), rs.string("document"))).list().apply()
      }
    }

    override def withId(contentId: String): Option[ContentInformation] = {
      DB readOnly { implicit session =>
        sql"select document from contentdata where id = ${contentId.toInt}".map(rs => rs.string("document")).single.apply match {
          case Some(json) => Option(asContentInformation(contentId, json))
          case None => None
        }
      }
    }

    override def exists(externalId: String): Boolean = {
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
