package no.ndla.contentapi.service

import java.net.URL
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.model._
import no.ndla.contentapi.integration.{AmazonClientComponent, AudioMeta}

trait StorageService {
  this: AmazonClientComponent =>
  val storageService: AmazonStorageService

  class AmazonStorageService {
    def uploadAudiofromUrl(storageKeyPrefix: String, audioMeta: AudioMeta): String = {
      val storageKey = s"${storageKeyPrefix}/${audioMeta.filename}"
      val audioStream = new URL(audioMeta.url).openStream()
      val metaData = new ObjectMetadata()
      metaData.setContentType(audioMeta.mimetype)
      metaData.setContentLength(audioMeta.fileSize.toLong)

      val request = new PutObjectRequest(storageName, storageKey, audioStream, metaData)
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
