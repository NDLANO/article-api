package no.ndla.articleapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.integration.DataSource
import no.ndla.articleapi.model.domain.Concept
import org.json4s.Formats
import org.postgresql.util.PGobject
import org.json4s.native.Serialization.write
import scalikejdbc._

trait ConceptRepository {
  this: DataSource =>
  val conceptRepository: ConceptRepository

  class ConceptRepository extends LazyLogging {
    implicit val formats: Formats = org.json4s.DefaultFormats + Concept.JSonSerializer

    def insertWithExternalId(article: Concept, externalId: String)(implicit session: DBSession = AutoSession): Concept = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(article))

      val articleId: Long = sql"insert into ${Concept.table} (document, external_id) values (${dataObject}, ${externalId})".updateAndReturnGeneratedKey.apply

      logger.info(s"Inserted new concept: $articleId")
      article.copy(id=Some(articleId))
    }

    def withId(id: Long): Option[Concept] =
      conceptWhere(sqls"co.id=${id.toInt}")

    private def conceptWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[Concept] = {
      val co = Concept.syntax("co")
      sql"select ${co.result.*} from ${Concept.as(co)} where $whereClause".map(Concept(co)).single.apply()
    }

  }
}
