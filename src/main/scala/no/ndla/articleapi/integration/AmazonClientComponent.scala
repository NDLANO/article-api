/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.integration

import com.amazonaws.services.s3.AmazonS3Client

trait AmazonClientComponent {
  val amazonClient: AmazonS3Client
  val storageName: String
}
