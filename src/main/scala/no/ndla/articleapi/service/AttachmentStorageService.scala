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
import no.ndla.articleapi.integration.AmazonClient
import no.ndla.articleapi.model.domain.ContentFilMeta

import scala.util.Try

trait AttachmentStorageService {
  this: AmazonClient =>
  val attachmentStorageService: AmazonStorageService

  class AmazonStorageService extends LazyLogging {
    def uploadFileFromUrl(filMeta: ContentFilMeta): Try[String] = {
      val storageKey = s"${filMeta.fileName}"
      val connection = filMeta.url.openConnection()
      val metaData = new ObjectMetadata()
      metaData.setContentType(filMeta.mimeType)
      metaData.setContentLength(filMeta.fileSize.toLong)

      uploadFile(new PutObjectRequest(attachmentStorageName, storageKey, connection.getInputStream, metaData), storageKey)
    }

  def uploadFile(request: PutObjectRequest, storageKey: String): Try[String] =
    Try(amazonClient.putObject(request)).map(_ => storageKey)

    def contains(storageKey: String): Boolean = {
      try {
        val s3Object = Option(amazonClient.getObject(new GetObjectRequest(attachmentStorageName, storageKey)))
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
