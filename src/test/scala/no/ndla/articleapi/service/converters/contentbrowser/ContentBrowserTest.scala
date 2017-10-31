/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class ContentBrowserTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
  val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
  val LeftMarginContentBrowser = s"[contentbrowser ==nid=$nodeId==imagecache=Hoyrespalte==width=180==alt=Enkle munnbind. Foto.==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text=Enkle munnbind.==link_text=Enkle munnbind.==text_align=right==css_class===css_class=contentbrowser contentbrowser_margin_left]"
  val RightContentBrowser = "[contentbrowser ==nid=24683==imagecache=Hoyrespalte==width===alt=tikroning, mynt. Foto.==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align=left==css_class===css_class=contentbrowser contentbrowser_margin_right]"
  val validContentBrowser = ContentBrowser(contentString, "nb")
  val altValidContentBrowser = ContentBrowser(LeftMarginContentBrowser, "nb")
  val alt2ValidContentBrowser = ContentBrowser(RightContentBrowser, "nb")
  val invalidContentBrowser = ContentBrowser(contentString.substring(1), "nb")

  test("That isContentBrowserField returns true for a valid contentbrowser string") {
    validContentBrowser.IsContentBrowserField should equal(true)
    altValidContentBrowser.IsContentBrowserField should equal(true)
    alt2ValidContentBrowser.IsContentBrowserField should equal(true)
  }

  test("That isContentBrowserField returns true for an invalid contentbrowser string") {
    invalidContentBrowser.IsContentBrowserField should equal(false)
  }

  test("That getStartEndIndex returns the correct start and end indexes") {
    validContentBrowser.StartEndIndex should equal((0, 382))
    ContentBrowser("Innhold! " + contentString + "also junk", "nb").StartEndIndex should equal((9, 391))

    altValidContentBrowser.StartEndIndex should equal((0, 403))
    ContentBrowser("junk junk" + LeftMarginContentBrowser + "also junk", "nb").StartEndIndex should equal((9, 412))
  }

  test("That get returns the associated value if the key exists") {
    validContentBrowser.get("nid") should equal(nodeId)
    validContentBrowser.get("imagecache") should equal("Fullbredde")
    validContentBrowser.get("link") should equal("")
    validContentBrowser.get("node_link") should equal("1")

    altValidContentBrowser.get("nid") should equal(nodeId)
  }

  test("That get returns the an empty string if the key does not exist") {
    validContentBrowser.get("invalid key") should equal("")
    validContentBrowser.get("") should equal("")
  }

}
