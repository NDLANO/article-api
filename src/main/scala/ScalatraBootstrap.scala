import javax.servlet.ServletContext

import no.ndla.articleapi.ComponentRegistry.{internController, contentController, resourcesApp}
import no.ndla.articleapi.ContentSwagger
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  implicit val swagger = new ContentSwagger

  override def init(context: ServletContext) {
    context.mount(contentController, "/content", "content")
    context.mount(resourcesApp, "/api-docs")
    context.mount(internController, "/intern")
  }

}
