/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import com.amazonaws.AmazonClientException
import com.amazonaws.services.s3.model.PutObjectRequest
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._

class StorageServiceTest extends UnitSuite with TestEnvironment {
  override val storageService = new AmazonStorageService

  test("That uploadFile returns None if upload fails") {
    val request = mock[PutObjectRequest]
    val key = "storagekey"

    when(amazonClient.putObject(request)).thenThrow(new AmazonClientException("Fail"))
    storageService.uploadFile(request, key) should equal (None)
  }

  test("That uploadFile returns the second parameter on success") {
    val key = "storagekey"
    storageService.uploadFile(mock[PutObjectRequest], key) should equal (Some(key))
  }
}
