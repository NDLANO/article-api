package no.ndla.articleapi.model.api

import com.scalatsi._
import no.ndla.articleapi.model.domain

/**
  * The `scala-tsi` plugin is not always able to derive the types that are used in `Seq` or other generic types.
  * Therefore we need to explicitly load the case classes here.
  * This is only necessary if the `sbt generateTypescript` script fails.
  */
object TSTypes {
  implicit val relatedContent: TSIType[RelatedContentLink] = TSType.fromCaseClass[RelatedContentLink]
  implicit val availability: TSType[domain.Availability.Value] =
    TSType.sameAs[domain.Availability.Value, domain.Availability.type]
}
