package no.ndla.contentapi.integration

import no.ndla.contentapi.model.RequiredLibrary
import org.jsoup.nodes.Element

import scala.collection.mutable.ListBuffer

trait ConverterModule {
  def convert(el: Element, requiredLibraries: ListBuffer[RequiredLibrary], errList: ListBuffer[String]): Element
}
