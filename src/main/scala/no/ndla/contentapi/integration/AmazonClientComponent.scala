package no.ndla.contentapi.integration

import com.amazonaws.services.s3.AmazonS3Client

trait AmazonClientComponent {
  val amazonClient: AmazonS3Client
  val storageName: String
}
