/*
 * Part of NDLA Content-API. API for searching and downloading content from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
import javax.servlet.ServletContext

import no.ndla.contentapi.{ResourcesApp, ContentSwagger, ContentController}
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  implicit val swagger = new ContentSwagger

  override def init(context: ServletContext) {
    context.mount(new ContentController, "/content", "content")
    context.mount(new ResourcesApp, "/api-docs")
  }

}
