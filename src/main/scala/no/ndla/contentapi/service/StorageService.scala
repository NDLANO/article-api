package no.ndla.contentapi.service

import java.net.URL
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.model._
import no.ndla.contentapi.integration.{AmazonClientComponent, AudioMeta}
import no.ndla.contentapi.ContentApiProperties.{ndlaUserName, ndlaPassword}
import scalaj.http.Base64

trait StorageService {
  this: AmazonClientComponent =>
  val storageService: AmazonStorageService

  class AmazonStorageService {
    def uploadAudiofromUrl(storageKeyPrefix: String, audioMeta: AudioMeta): String = {
      val storageKey = s"${storageKeyPrefix}/${audioMeta.filename}"
      val audioConnection = new URL(audioMeta.url).openConnection()
      audioConnection.setRequestProperty ("Authorization", s"Basic ${Base64.encodeString(s"$ndlaUserName:$ndlaPassword")}")
      val metaData = new ObjectMetadata()
      metaData.setContentType(audioMeta.mimetype)
      metaData.setContentLength(audioMeta.fileSize.toLong)

      val request = new PutObjectRequest(storageName, storageKey, audioConnection.getInputStream(), metaData)
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
