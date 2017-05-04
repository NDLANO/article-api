package no.ndla.articleapi.model.domain

import no.ndla.articleapi.service.converters.{Attributes, ResourceType}

object EmbedTag {
  def requiredAttributesForResourceType(resourceType: ResourceType.Value): Set[Attributes.Value] =
    requiredAttributesForAllResourceTypes ++
    Map(ResourceType.Image -> requiredAttributesForImageEmbedTag,
      ResourceType.Audio -> requiredAttributesForAudioEmbedTag,
      ResourceType.H5P -> requiredAttributesForH5PEmbedTag,
      ResourceType.Brightcove -> requiredAttributesForBrightCoveEmbedTag,
      ResourceType.ContentLink -> requiredAttributesForContentLink,
      ResourceType.Error -> requiredAttributesForError,
      ResourceType.ExternalContent -> requiredAttributesForExternalContent,
      ResourceType.NRKContent -> requiredAttributesForNrkContent
    ).getOrElse(resourceType, requiredAttributesForAllResourceTypes)

  private val requiredAttributesForAllResourceTypes = Set(Attributes.DataResource)

  private val requiredAttributesForImageEmbedTag =
    Set(Attributes.DataResource_Id,
      Attributes.DataSize,
      Attributes.DataAlt,
      Attributes.DataCaption,
      Attributes.DataAlign)

  private val requiredAttributesForAudioEmbedTag =
    Set(Attributes.DataResource_Id)

  private val requiredAttributesForH5PEmbedTag =
    Set(Attributes.DataUrl)

  private val requiredAttributesForBrightCoveEmbedTag =
    Set(Attributes.DataCaption,
      Attributes.DataVideoId,
      Attributes.DataAccount,
      Attributes.DataPlayer)

  private val requiredAttributesForContentLink =
    Set(Attributes.DataContentId,
      Attributes.DataLinkText)

  private val requiredAttributesForError =
    Set(Attributes.DataMessage)

  private val requiredAttributesForExternalContent =
    Set(Attributes.DataUrl)

  private val requiredAttributesForNrkContent =
    Set(Attributes.DataNRKVideoId,
      Attributes.DataUrl)
}
