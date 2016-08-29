/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters

import no.ndla.articleapi.integration.{ConverterModule, LanguageContent}
import no.ndla.articleapi.model.{ImportStatus, NodeIngress}
import no.ndla.articleapi.service.{ExtractServiceComponent, ImageApiServiceComponent}

trait IngressConverter {
  this: ExtractServiceComponent with ImageApiServiceComponent =>
  val ingressConverter: IngressConverter

  class IngressConverter extends ConverterModule {

    override def convert(content: LanguageContent, importStatus: ImportStatus): (LanguageContent, ImportStatus) = {
      if (content.containsIngress) {
        return (content, importStatus)
      }

      content.ingress match {
        case Some(ingress) => {
          ingress.ingressVisPaaSiden match {
            case 0 => (content.copy(containsIngress = true), importStatus)
            case _ => {
              val element = stringToJsoupDocument(content.content)
              val (ingressContent, status) = createIngress(ingress)
              element.prepend(ingressContent)
              (content.copy(content = jsoupDocumentToString(element), containsIngress = true), importStatus.copy(messages = importStatus.messages ++ status.messages))
            }
          }
        }
        case None => (content.copy(containsIngress = true), importStatus)
      }
    }

    private def createIngress(ingress: NodeIngress): (String, ImportStatus) = {
      val (imageTag, message) = ingress.imageNid match {
        case Some(imageId) => {
          imageApiService.importOrGetMetaByExternId(imageId) match {
            case Some(image) => (s"""<img src="/images/${image.images.full.get.url}" />""", ImportStatus(Seq(), Seq()))
            case None => {
              ("", ImportStatus(s"Image with id $imageId was not found", Seq()))
            }
          }
        }
        case None => ("", ImportStatus(Seq(), Seq()))
      }
      (s"<section>$imageTag ${ingress.content}</section>", message)
    }
  }
}
