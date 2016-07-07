package no.ndla.contentapi.service.converters.contentbrowser

case class ContentBrowser(contentBrowserString: String, language: Option[String], id: Int) {
  // Extract the contentbrowser variables
  private val Pattern = """(?s).*\[contentbrowser (.*) ?contentbrowser\].*""".r
  private val ContentField = contentBrowserString match {
    case Pattern(group) => group
    case _ => ""
  }

  // Extract every key-value pair and build a map
  private val KeyVal = ContentField.split("==").map(x => x.stripPrefix("=").split("="))
  private val FieldMap = KeyVal.map(el => el(0) -> (if (el.length > 1) el(1) else "")).toMap

  def isContentBrowserField(): Boolean = {
    contentBrowserString.matches(Pattern.toString)
  }

  def getStartEndIndex(): (Int, Int) = {
    val (startIdf, endIdf) = ("[contentbrowser ", "contentbrowser]")
    val a = contentBrowserString.indexOf(ContentField)
    (a - startIdf.length(), a + ContentField.length() + endIdf.length())
  }

  def get(key: String): String = {
    FieldMap.getOrElse(key, "")
  }
}

object ContentBrowser {
  private var count = 0

  def apply(contentBrowserString: String, language: Option[String]): ContentBrowser = {
    count += 1
    ContentBrowser(contentBrowserString, language, count)
  }

  def reset = {
    count = 0
  }
}
