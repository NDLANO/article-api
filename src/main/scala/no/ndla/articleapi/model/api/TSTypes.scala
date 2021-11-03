package no.ndla.articleapi.model.api

import com.scalatsi._
import no.ndla.validation.ValidationMessage
import no.ndla.articleapi.model.domain

/**
  * The `scala-tsi` plugin is not always able to derive the types that are used in `Seq` or other generic types.
  * Therefore we need to explicitly load the case classes here.
  * This is only necessary if the `sbt generateTypescript` script fails.
  */
object TSTypes {
  implicit val author: TSIType[Author] = TSType.fromCaseClass[Author]
  implicit val requiredLibrary: TSIType[RequiredLibrary] = TSType.fromCaseClass[RequiredLibrary]
  implicit val relatedContent: TSIType[RelatedContentLink] = TSType.fromCaseClass[RelatedContentLink]
  implicit val articleSummaryV2: TSIType[ArticleSummaryV2] = TSType.fromCaseClass[ArticleSummaryV2]
  implicit val articleV2: TSIType[ArticleV2] = TSType.fromCaseClass[ArticleV2]
  implicit val validationMessage: TSIType[ValidationMessage] = TSType.fromCaseClass[ValidationMessage]
  implicit val articleTag: TSIType[domain.ArticleTag] = TSType.fromCaseClass[domain.ArticleTag]
  implicit val articleMetaDescription: TSIType[domain.ArticleMetaDescription] =
    TSType.fromCaseClass[domain.ArticleMetaDescription]
  implicit val availability: TSType[domain.Availability.type] = TSType.scalaEnumTSType[domain.Availability.type]
  implicit val partialPublishArticle: TSIType[PartialPublishArticle] = {
    implicit val availability: TSType[domain.Availability.Value] = TSType.external("Availability")
    TSType.fromCaseClass[PartialPublishArticle]
  }
}
