package no.ndla.contentapi.batch

import no.ndla.contentapi.ComponentRegistry
import no.ndla.contentapi.model._

object TestdataUploader {

  val testdata = List(
    ("1",
      ContentInformation("0",
        List(ContentTitle("Myklesaken splittet Norge", Some("nb"))),
        List(Content(io.Source.fromInputStream(getClass.getResourceAsStream(s"/testdata/1.html")).mkString, Some("nb"))),
        Copyright(License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")), "NTB Tema", List(Author("forfatter", "Ingrid Brubaker"))),
        List(ContentTag("myklesaken", Some("nb")), ContentTag("norge", Some("nb"))), List())),

    ("2",ContentInformation("0",
      List(ContentTitle("Hva er utholdenhet", Some("nb"))),
      List(Content(io.Source.fromInputStream(getClass.getResourceAsStream(s"/testdata/2.html")).mkString, Some("nb"))),
      Copyright(License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")), "Ukjent", List(Author("forfatter", "Oddbjørg Vatn Slapgaard"))),
      List(ContentTag("utholdenhet", Some("nb")), ContentTag("aerob", Some("nb"))), List(RequiredLibrary("text/javascript", "H5P-Resizer", "http://ndla.no/sites/all/modules/h5p/library/js/h5p-resizer.js")))),

    ("3", ContentInformation("0",
      List(ContentTitle("Potenser", Some("nb"))),
      List(Content(io.Source.fromInputStream(getClass.getResourceAsStream(s"/testdata/3.html")).mkString, Some("nb"))),
      Copyright(License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")), "Ukjent", List(Author("forfatter", "Noen"))),
      List(ContentTag("potenser", Some("nb")), ContentTag("matematikk", Some("nb"))), List(RequiredLibrary("text/javascript", "MathJax", "https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML")))),

    ("4", ContentInformation("0",
      List(ContentTitle("Bygg fordøyelsessystemet", Some("nb"))),
      List(Content(io.Source.fromInputStream(getClass.getResourceAsStream(s"/testdata/4.html")).mkString, Some("nb"))),
      Copyright(License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")), "Ukjent", List(Author("forfatter", "Amendor"))),
      List(ContentTag("fordøyelsessystemet", Some("nb"))), List())),

    ("5", ContentInformation("0",
      List(ContentTitle("And the millionth word is...", Some("en"))),
      List(Content(io.Source.fromInputStream(getClass.getResourceAsStream(s"/testdata/5.html")).mkString, Some("nb"))),
      Copyright(License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")), "Ukjen  t", List(Author("forfatter", "Marion Federi"))),
      List(ContentTag("oxford", Some("en"))), List())),

    ("6", ContentInformation("0",
      List(ContentTitle("Canada - What is Really Canadian?", Some("en"))),
      List(Content(io.Source.fromInputStream(getClass.getResourceAsStream(s"/testdata/6.html")).mkString, Some("nb"))),
      Copyright(License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")), "Ukjent", List(Author("forfatter", "Marion Federi"))),
      List(ContentTag("Canada", Some("en"))), List(RequiredLibrary("text/javascript", "H5P-Resizer", "http://ndla.no/sites/all/modules/h5p/library/js/h5p-resizer.js"))))
  )

  def main(args: Array[String]) {
    val contentData = ComponentRegistry.contentRepository

    testdata.foreach(tuppel => {
      if(!contentData.exists(tuppel._1))
        contentData.insert(tuppel._2, tuppel._1)
      else
        contentData.update(tuppel._2, tuppel._1)
    })
  }
}
