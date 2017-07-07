/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


import javax.servlet.ServletContext

import no.ndla.articleapi.ComponentRegistry.{internController, articleController, articleControllerV2, resourcesApp, healthController}
import no.ndla.articleapi.ArticleSwagger
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  implicit val swagger = new ArticleSwagger

  override def init(context: ServletContext) {
    context.mount(articleController, "/article-api/v1/articles", "articles")
    context.mount(articleControllerV2, "/article-api/v2/articles", "articlesV2")
    context.mount(resourcesApp, "/article-api/api-docs")
    context.mount(internController, "/intern")
    context.mount(healthController, "/health")
  }

}
