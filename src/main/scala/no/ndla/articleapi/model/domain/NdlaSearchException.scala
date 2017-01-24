/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model.domain

import io.searchbox.client.JestResult

class NdlaSearchException(jestResponse: JestResult) extends RuntimeException(jestResponse.getErrorMessage) {
  def getResponse: JestResult = jestResponse
}
