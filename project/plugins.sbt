// We need this while using the NDLA fork of the scalapact plugin
resolvers ++= scala.util.Properties
  .envOrNone("NDLA_RELEASES")
  .map(repo => "Release Sonatype Nexus Repository Manager" at repo)
  .toSeq

addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "4.0.2")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.5.1")
addSbtPlugin("com.itv" % "sbt-scalapact" % "2.3.3-NDLA")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")
