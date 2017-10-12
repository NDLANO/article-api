/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.integration.DataSource
import no.ndla.articleapi.model.domain.Concept
import org.json4s.Formats
import org.postgresql.util.PGobject
import org.json4s.native.Serialization.write
import scalikejdbc._

import scala.util.{Failure, Success, Try}

trait ConceptRepository {
  this: DataSource =>
  val conceptRepository: ConceptRepository

  class ConceptRepository extends LazyLogging with Repository[Concept] {
    implicit val formats: Formats = org.json4s.DefaultFormats + Concept.JSonSerializer

    def insertWithExternalId(concept: Concept, externalId: String)(implicit session: DBSession = AutoSession): Concept = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(concept))

      val conceptId: Long = sql"insert into ${Concept.table} (document, external_id) values (${dataObject}, ${externalId})".updateAndReturnGeneratedKey.apply

      logger.info(s"Inserted new concept: $conceptId")
      concept.copy(id=Some(conceptId))
    }

    def insertWithoutContent(externalId: String)(implicit session: DBSession = AutoSession): Long = {
      val startRevision = 1

      val conceptId: Long = sql"insert into ${Concept.table} (external_id) values (${externalId})".updateAndReturnGeneratedKey().apply

      logger.info(s"Inserted empty concept $externalId: $conceptId")
      conceptId
    }

    def updateWithExternalId(concept: Concept, externalId: String)(implicit session: DBSession = AutoSession): Try[Concept] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(concept))

      Try(sql"update ${Concept.table} set document=${dataObject} where external_id=${externalId}".updateAndReturnGeneratedKey.apply) match {
        case Success(id) => Success(concept.copy(id=Some(id)))
        case Failure(ex) =>
          logger.warn(s"Failed to update concept with external id $externalId: ${ex.getMessage}")
          Failure(ex)
      }
    }

    def withId(id: Long): Option[Concept] =
      conceptWhere(sqls"co.id=${id.toInt}")

    def withExternalId(externalId: String): Option[Concept] =
      conceptWhere(sqls"co.external_id=$externalId")

    def getIdFromExternalId(externalId: String)(implicit session: DBSession = AutoSession): Option[Long] =
      sql"select id from ${Concept.table} where external_id=${externalId}".map(rs => rs.long("id")).single.apply()

    def exists(externalId: String): Boolean =
      conceptWhere(sqls"co.external_id=$externalId").isDefined

    def delete(id: Long)(implicit session: DBSession = AutoSession) =
      sql"delete from ${Concept.table} where id = $id".update().apply

    override def minMaxId(implicit session: DBSession = AutoSession): (Long, Long) = {
      sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${Concept.table}".map(rs => {
        (rs.long("mi"), rs.long("ma"))
      }).single().apply() match {
        case Some(minmax) => minmax
        case None => (0L, 0L)
      }
    }

    override def documentsWithIdBetween(min: Long, max: Long): List[Concept] =
      conceptsWhere(sqls"co.id between $min and $max")

    private def conceptWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[Concept] = {
      val co = Concept.syntax("co")
      sql"select ${co.result.*} from ${Concept.as(co)} where $whereClause".map(Concept(co)).single.apply()
    }

    private def conceptsWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): List[Concept] = {
      val co = Concept.syntax("co")
      sql"select ${co.result.*} from ${Concept.as(co)} where $whereClause".map(Concept(co)).list.apply()
    }

  }
}
