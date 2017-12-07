/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters.contentbrowser

import com.netaporter.uri.dsl._
import com.netaporter.uri.Uri.parse
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.api.ImportException
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.ExtractService
import no.ndla.articleapi.service.converters.HtmlTagGenerator
import no.ndla.validation.ResourceType
import org.jsoup.Jsoup

import scala.util.{Failure, Success, Try}

trait LenkeConverterModule {
  this: ExtractService with HtmlTagGenerator =>

  object LenkeConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "lenke"

    override def convert(content: ContentBrowser, importStatus: ImportStatus): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      logger.info(s"Converting lenke with nid ${content.get("nid")}")

      convertLink(content) match {
        case Success((linkHtml, requiredLibraries, errors)) => Success(linkHtml, requiredLibraries, importStatus.addMessages(errors))
        case Failure(x) => Failure(x)
      }
    }

    def convertLink(cont: ContentBrowser): Try[(String, Seq[RequiredLibrary], Seq[String])] = {
      extractService.getNodeEmbedMeta(cont.get("nid")).map(meta => {
        val (url, embedCode) = (meta.url.getOrElse(""), meta.embedCode.getOrElse(""))
        val (htmlTag, requiredLibrary, errors) = cont.get("insertion") match {
          case "link" => insertAnchor(url, cont)
          case "inline" => insertInline(url, embedCode, cont)
          case "lightbox_large" => insertAnchor(url, cont)
          case "collapsed_body" => insertAnchor(url, cont)
          case _ => insertUnhandled(url, cont)
        }

        val NDLAPattern = """.*(ndla.no).*""".r
        val warnings =  Try(parse(url)) match {
          case Success(uri) => uri.host.getOrElse("") match {
            case NDLAPattern(_) => Seq(s"Link to NDLA old resource: '$url'")
            case _ => Seq()
          }
          case Failure(_) => Seq(s"Link in article is invalid: '$url'")
        }

        warnings.foreach(msg => logger.warn(msg))
        (htmlTag, requiredLibrary.toList, errors ++ warnings)
      }) match {
        case Success(x) => Success(x)
        case Failure(_) => Failure(ImportException(s"Failed to import embed metadata for node id ${cont.get("nid")}"))
      }
    }

    private def insertInline(url: String, embedCode: String, cont: ContentBrowser): (String, Option[RequiredLibrary], Seq[String]) = {
      val message = s"External resource to be embedded: $url"
      logger.info(message)

      val NRKUrlPattern = """(.*\.?nrk.no)""".r
      val PreziUrlPattern = """(.*\.?prezi.com)""".r
      val CommonCraftUrlPattern = """(.*\.?commoncraft.com)""".r
      val NdlaFilmIundervisningUrlPattern = """(.*\.?ndla.filmiundervisning.no)""".r
      val KahootUrlPattern = """(.*\.?play.kahoot.it)""".r
      val vimeoProUrlPattern = """(.*\.?vimeopro.com)""".r
      val khanAcademyUrlPattern = """(.*\.?khanacademy.org)""".r
      val kunnskapsFilmUrlPattern = """(.*\.?kunnskapsfilm.no)""".r
      val tv2SkoleUrlPattern = """(.*\.?tv2skole.no)""".r
      val vgNoUrlPattern = """(.*\.?vg.no)""".r
      val scribdUrlPattern = """(.*\.?scribd.com)""".r

      val (embedTag, requiredLibs) = url.host.getOrElse("") match {
        case NRKUrlPattern(_) => getNrkEmbedTag(embedCode, url)
        case PreziUrlPattern(_) => getRegularEmbedTag(embedCode, ResourceType.Prezi)
        case CommonCraftUrlPattern(_) => getRegularEmbedTag(embedCode, ResourceType.Commoncraft)
        case NdlaFilmIundervisningUrlPattern(_) => getRegularEmbedTag(embedCode, ResourceType.NdlaFilmIundervisning)
        case KahootUrlPattern(_) => getRegularEmbedTag(embedCode, ResourceType.Kahoot)
        case vimeoProUrlPattern(_) => getVimeoProEmbedTag(embedCode)
        case kunnskapsFilmUrlPattern(_) => getKunnskapsFilmEmbedTag(embedCode)
        case khanAcademyUrlPattern(_) => getRegularEmbedTag(embedCode, ResourceType.KhanAcademy)
        case tv2SkoleUrlPattern(_) => getRegularEmbedTag(embedCode, ResourceType.Tv2Skole)
        case vgNoUrlPattern(_) => getRegularEmbedTag(embedCode, ResourceType.VgNo)
        case scribdUrlPattern(_) => getRegularEmbedTag(embedCode, ResourceType.Scribd)
        case _ => (HtmlTagGenerator.buildExternalInlineEmbedContent(url), None)
      }
      (embedTag, requiredLibs, message :: Nil)
    }

    def getNrkEmbedTag(embedCode: String, url: String): (String, Option[RequiredLibrary]) = {
      val doc = Jsoup.parseBodyFragment(embedCode)
      val (videoId, requiredLibraryUrl) = (doc.select("div[data-nrk-id]").attr("data-nrk-id"), doc.select("script").attr("src"))
      val requiredLibrary = RequiredLibrary("text/javascript", "NRK video embed", requiredLibraryUrl.copy(scheme=None))

      (HtmlTagGenerator.buildNRKInlineVideoContent(videoId, url), Some(requiredLibrary))
    }

    def getRegularEmbedTag(embedCode: String, resourceType: ResourceType.Value): (String, Option[RequiredLibrary]) = {
      val doc = Jsoup.parseBodyFragment(embedCode).select("iframe").first()
      val (src, width, height) = (doc.attr("src"), doc.attr("width"), doc.attr("height"))

      (HtmlTagGenerator.buildRegularInlineContent(src, width, height, resourceType), None)
    }

    def getVimeoProEmbedTag(embedCode: String): (String, Option[RequiredLibrary]) = {
      val doc = Jsoup.parseBodyFragment(embedCode).select("iframe").first()
      val src = doc.attr("src")

      (HtmlTagGenerator.buildExternalInlineEmbedContent(src), None)
    }

    def getKunnskapsFilmEmbedTag(embedCode: String): (String, Option[RequiredLibrary]) = {
      val doc = Jsoup.parseBodyFragment(embedCode).select("iframe").first()
      val src = doc.attr("src")

      // Since all sources seem to be vimeo urls, we simply use data-resource=external to let oembed-proxy handle these
      (HtmlTagGenerator.buildExternalInlineEmbedContent(src), None)
    }

    private def insertDetailSummary(url: String, embedCode: String, cont: ContentBrowser): (String, Option[RequiredLibrary], Seq[String]) = {
      val (elementToInsert, requiredLib, figureErrors) = insertInline(url, embedCode, cont)
      (s"<details><summary>${cont.get("link_text")}</summary>$elementToInsert</details>", requiredLib, figureErrors)
    }

    private def insertAnchor(url: String, cont: ContentBrowser): (String, Option[RequiredLibrary], Seq[String]) = {
      val htmlTag = HtmlTagGenerator.buildAnchor(url, cont.get("link_text"), cont.get("link_title_text"), true)
      (s" $htmlTag", None, Seq())
    }

    private def insertUnhandled(url: String, cont: ContentBrowser): (String, Option[RequiredLibrary], Seq[String]) = {
      val (anchor, requiredLib, anchorErrors) = insertAnchor(url, cont)
      val message = s"""Unhandled insertion method '${cont.get("insertion")}' on '${cont.get("link_text")}'. Defaulting to link."""

      logger.warn(message)
      (anchor, requiredLib, anchorErrors :+ message)
    }
  }

}
