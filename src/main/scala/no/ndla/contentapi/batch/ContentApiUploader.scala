package no.ndla.contentapi.batch

import no.ndla.contentapi.integration.AmazonIntegration
import no.ndla.contentapi.model._

object ContentApiUploader {


  val testdata = List(
    ("1",
      ContentInformation("0",
        List(ContentTitle("Myklesaken splittet Norge", Some("nb"))),
        io.Source.fromInputStream(getClass.getResourceAsStream(s"/testdata/1.html")).mkString,
        Copyright(License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")), "NTB Tema", List(Author("forfatter", "Ingrid Brubaker"))),
        List(ContentTag("myklesaken", Some("nb")), ContentTag("norge", Some("nb"))), List())),

    ("2",ContentInformation("0",
      List(ContentTitle("Hva er utholdenhet", Some("nb"))),
      io.Source.fromInputStream(getClass.getResourceAsStream(s"/testdata/2.html")).mkString,
      Copyright(License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")), "Ukjent", List(Author("forfatter", "Oddbjørg Vatn Slapgaard"))),
      List(ContentTag("utholdenhet", Some("nb")), ContentTag("aerob", Some("nb"))), List(RequiredLibrary("text/javascript", "H5P-Resizer", "http://ndla.no/sites/all/modules/h5p/library/js/h5p-resizer.js")))),

    ("3", ContentInformation("0",
      List(ContentTitle("Potenser", Some("nb"))),
      io.Source.fromInputStream(getClass.getResourceAsStream(s"/testdata/3.html")).mkString,
      Copyright(License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")), "Ukjent", List(Author("forfatter", "Noen"))),
      List(ContentTag("potenser", Some("nb")), ContentTag("matematikk", Some("nb"))), List(RequiredLibrary("text/javascript", "MathJax", "https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML")))),

    ("4", ContentInformation("0",
      List(ContentTitle("Bygg fordøyelsessystemet", Some("nb"))),
      io.Source.fromInputStream(getClass.getResourceAsStream(s"/testdata/4.html")).mkString,
      Copyright(License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")), "Ukjent", List(Author("forfatter", "Amendor"))),
      List(ContentTag("fordøyelsessystemet", Some("nb"))), List()))
  )

  def main(args: Array[String]) {
    val contentData = AmazonIntegration.getContentData()

    testdata.foreach(tuppel => {
      contentData.insert(tuppel._2, tuppel._1)
    })
  }
}
