package no.ndla.articleapi.model.domain

import java.util.Date

case class ImportError(description: String = "Error during import", messages: Seq[String], occuredAt: Date = new Date())

