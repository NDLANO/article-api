package no.ndla.articleapi.model.search

import org.json4s.JsonAST.{JArray, JField, JObject, JString}
import org.json4s.{CustomSerializer, MappingException}
import no.ndla.articleapi.model.domain.Language.UnknownLanguage

class SearchableLanguageValueSerializer extends CustomSerializer[SearchableLanguageValues](format => ( {
  case JObject(items) => SearchableLanguageValues(items.map {
    case JField(name, JString(value)) => LanguageValue(name, value)
  })
}, {
  case x: SearchableLanguageValues =>
    JObject(x.languageValues.map(languageValue => JField(languageValue.lang, JString(languageValue.value))).toList)
}))


class SearchableLanguageListSerializer extends CustomSerializer[SearchableLanguageList](format => ( {
  case JObject(items) => {
    SearchableLanguageList(items.map{
      case JField(name, JArray(fieldItems)) => LanguageValue(name, fieldItems.map{
        case JString(value) => value
        case x => throw new MappingException(s"Cannot convert $x to SearchableLanguageList")
      }.to[Seq])
    })
  }
}, {
  case x: SearchableLanguageList =>
    JObject(x.languageValues.map(languageValue => JField(languageValue.lang, JArray(languageValue.value.map(lv => JString(lv)).toList))).toList)
}))

object SearchableLanguageFormats {
  val JSonFormats = org.json4s.DefaultFormats + new SearchableLanguageValueSerializer + new SearchableLanguageListSerializer
}
