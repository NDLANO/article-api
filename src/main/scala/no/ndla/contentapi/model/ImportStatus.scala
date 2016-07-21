package no.ndla.contentapi.model

case class ImportStatus(messages: Seq[String], visitedNodes: Seq[String] = Seq())
object ImportStatus {
  def apply(message: String, visitedNodes: Seq[String]): ImportStatus = ImportStatus(Seq(message), visitedNodes)
}
