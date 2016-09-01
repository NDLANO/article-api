package db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V3__ChangeContentNameTest extends UnitSuite with TestEnvironment {
  val migrator = new V3__ChangeContentName

  test("That convertDocumentToNewFormat with no content returns empty article list") {
    val content = V3_DBContent (1, """{"content":[]}""")
    val expectedResult = V3_DBContent (1, """{"article":[]}""")
    val optConverted = migrator.convertDocumentToNewFormat(content)

    optConverted.isDefined should be(true)
    optConverted.get.document should equal(expectedResult.document)
  }


  test("That converting an already converted content node returns none") {
    val content = V3_DBContent(2, """{"article":[{"article":"<section><section>","language":"nb"}]}""")
    migrator.convertDocumentToNewFormat(content) should be(None)
  }

  test("That convertDocumentToNewFormat converts to expected format") {
    val before = """{"content":[{"content":"<section><section>","language":"nb"}]}"""
    val expectedAfter = """{"article":[{"article":"<section><section>","language":"nb"}]}"""
    val content = V3_DBContent(3, before)

    val optConverted = migrator.convertDocumentToNewFormat(content)

    println(optConverted.get.document)
    optConverted.isDefined should be(true)
    optConverted.get.document should equal(expectedAfter)
  }

}
