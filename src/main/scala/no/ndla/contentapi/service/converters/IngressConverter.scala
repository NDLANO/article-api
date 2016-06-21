package no.ndla.contentapi.service.converters

import no.ndla.contentapi.integration.{ConverterModule, LanguageContent, NodeIngress}
import no.ndla.contentapi.model.ImportStatus
import no.ndla.contentapi.service.{ExtractServiceComponent, ImageApiServiceComponent}

trait IngressConverter {
  this: ExtractServiceComponent with ImageApiServiceComponent =>
  val ingressConverter: IngressConverter

  class IngressConverter extends ConverterModule {

    override def convert(content: LanguageContent): (LanguageContent, ImportStatus) = {
      extractService.getNodeIngress(content.nodeId) match {
        case Some(ingress) => {

          ingress.ingressVisPaaSiden match {
            case 0 => return (content, ImportStatus())
            case _ => {
              val element = stringToJsoupDocument(content.content)
              element.children().first().before(createIngress(ingress))
              (content.copy(content = jsoupDocumentToString(element)), ImportStatus())
            }
          }
        }
        case None => (content, ImportStatus())
      }
    }

    private def createIngress(ingress: NodeIngress): String = {
      val imageTag = ingress.imageNid match {
        case Some(imageId) => {
          imageApiService.getMetaByExternId(imageId) match {
            case Some(image) => s"""<img src="/images/${image.images.full.get.url}" />"""
            case None => ""
          }
        }
        case None => ""
      }

      s"<section>$imageTag ${ingress.content}</section>"
    }
  }
}
