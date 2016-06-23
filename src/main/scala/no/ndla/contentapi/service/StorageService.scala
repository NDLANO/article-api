package no.ndla.contentapi.service

import java.net.URL
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.model._
import no.ndla.contentapi.integration.{AmazonClientComponent, ContentFilMeta}

trait StorageService {
  this: AmazonClientComponent =>
  val storageService: AmazonStorageService

  class AmazonStorageService {
    def uploadFileFromUrl(storageKeyPrefix: String, filMeta: ContentFilMeta): String = {
      val storageKey = s"${storageKeyPrefix}/${filMeta.fileName}"
      val connection = new URL(filMeta.url).openConnection()
      val metaData = new ObjectMetadata()
      metaData.setContentType(filMeta.mimeType)
      metaData.setContentLength(filMeta.fileSize.toLong)

      val request = new PutObjectRequest(storageName, storageKey, connection.getInputStream(), metaData)
      amazonClient.putObject(request)
      storageKey
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
