package no.ndla.contentapi.integration

import javax.sql.DataSource

trait DataSourceComponent {
  val dataSource: DataSource
}
