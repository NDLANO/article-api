package no.ndla.contentapi

import javax.sql.DataSource
import org.flywaydb.core.Flyway

object DBMigrator {
  def migrate(datasource: DataSource) = {
    val flyway = new Flyway()
    flyway.setDataSource(datasource)
    flyway.migrate()
  }
}
