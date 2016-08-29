/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.integration

import com.sksamuel.elastic4s.ElasticClient

trait ElasticClientComponent {
  val elasticClient: ElasticClient
}
