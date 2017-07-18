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

  class ConceptRepository extends LazyLogging {
    implicit val formats: Formats = org.json4s.DefaultFormats + Concept.JSonSerializer

    def insertWithExternalId(concept: Concept, externalId: String)(implicit session: DBSession = AutoSession): Concept = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(concept))

      val conceptId: Long = sql"insert into ${Concept.table} (document, external_id) values (${dataObject}, ${externalId})".updateAndReturnGeneratedKey.apply

      logger.info(s"Inserted new concept: $conceptId")
      concept.copy(id=Some(conceptId))
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

    private def conceptWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[Concept] = {
      val co = Concept.syntax("co")
      sql"select ${co.result.*} from ${Concept.as(co)} where $whereClause".map(Concept(co)).single.apply()
    }

  }
}
