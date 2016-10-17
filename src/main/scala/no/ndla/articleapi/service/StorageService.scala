/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import com.amazonaws.{AmazonClientException, AmazonServiceException}
import com.amazonaws.services.s3.model._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.integration.AmazonClientComponent
import no.ndla.articleapi.model.domain.ContentFilMeta

trait StorageService {
  this: AmazonClientComponent =>
  val storageService: AmazonStorageService

  class AmazonStorageService extends LazyLogging {
    def uploadFileFromUrl(storageKeyPrefix: String, filMeta: ContentFilMeta): Option[String] = {
      val storageKey = s"$storageKeyPrefix/${filMeta.fileName}"
      val connection = filMeta.url.openConnection()
      val metaData = new ObjectMetadata()
      metaData.setContentType(filMeta.mimeType)
      metaData.setContentLength(filMeta.fileSize.toLong)

      uploadFile(new PutObjectRequest(storageName, storageKey, connection.getInputStream, metaData), storageKey)
    }

  def uploadFile(request: PutObjectRequest, storageKey: String): Option[String] = {
    try {
      amazonClient.putObject(request)
      Some(storageKey)
    } catch {
      case ace @ (_: AmazonClientException | _: AmazonServiceException) => {
        logger.warn("Failed to upload file to S3")
        None
      }
    }
  }

    def contains(storageKey: String): Boolean = {
      try {
        val s3Object = Option(amazonClient.getObject(new GetObjectRequest(storageName, storageKey)))
        s3Object match {
          case Some(obj) => {
            obj.close()
            true
          }
          case None => false
        }
      } catch {
        case ase: AmazonServiceException => if (ase.getErrorCode == "NoSuchKey") false else throw ase
      }
    }
  }
}
