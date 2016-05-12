package no.ndla.contentapi.batch

import scala.io.Source.fromFile
import no.ndla.contentapi.batch.BatchComponentRegistry.{cmData, converterService};

object ContentUploader {
  def main(args: Array[String]) {
    val base = "content-api/src/main/scala/no/ndla/contentapi/batch/"
    val testContent = fromFile(base + "159967.html").mkString
    val content  = converterService.convert(testContent)
    println(content)
  }
}
