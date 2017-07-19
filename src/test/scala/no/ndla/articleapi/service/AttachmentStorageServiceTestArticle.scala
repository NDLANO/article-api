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

import scala.util.Success

class AttachmentStorageServiceTestArticle extends UnitSuite with TestEnvironment {
  override val attachmentStorageService = new AmazonStorageService

  test("That uploadFile returns None if upload fails") {
    val request = mock[PutObjectRequest]
    val key = "storagekey"

    when(amazonClient.putObject(request)).thenThrow(new AmazonClientException("Fail"))
    attachmentStorageService.uploadFile(request, key).isFailure should equal(true)
  }

  test("That uploadFile returns the second parameter on success") {
    val key = "storagekey"
    attachmentStorageService.uploadFile(mock[PutObjectRequest], key) should equal (Success(key))
  }
}
