package no.ndla.articleapi.integration

import com.sksamuel.elastic4s.ElasticClient

trait ElasticClientComponent {
  val elasticClient: ElasticClient
}
