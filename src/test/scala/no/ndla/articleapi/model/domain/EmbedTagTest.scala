package no.ndla.articleapi.model.domain

import no.ndla.articleapi.service.converters.ResourceType
import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class EmbedTagTest extends UnitSuite with TestEnvironment {

  test("Rules for all resource types should be defined") {
    val resourceTypesFromConfigFile = EmbedTag.requiredAttributesByResourceType.keys.toSet
    val resourceTypesFromEnumDeclaration = ResourceType.values.toSet

    resourceTypesFromEnumDeclaration should equal (resourceTypesFromConfigFile)
  }

  test("data-resource should be required for all resource types") {
    val resourceTypes = EmbedTag.requiredAttributesByResourceType.keys
    resourceTypes.foreach(resourceType => EmbedTag.requiredAttributesForResourceType(resourceType))
  }

}
