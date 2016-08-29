package no.ndla.articleapi.integration

import javax.sql.DataSource

trait DataSourceComponent {
  val dataSource: DataSource
}
