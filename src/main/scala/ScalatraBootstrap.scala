/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


import javax.servlet.ServletContext

import no.ndla.articleapi.ComponentRegistry.{internController, contentController, resourcesApp}
import no.ndla.articleapi.ArticleSwagger
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  implicit val swagger = new ArticleSwagger

  override def init(context: ServletContext) {
    context.mount(contentController, "/articles", "articles")
    context.mount(resourcesApp, "/api-docs")
    context.mount(internController, "/intern")
  }

}
