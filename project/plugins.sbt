// We need this while using the NDLA fork of the scalapact plugin
resolvers ++= scala.util.Properties
  .envOrNone("NDLA_RELEASES")
  .map(repo => "Release Sonatype Nexus Repository Manager" at repo)
  .toSeq

addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "4.0.2")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.5.1")
// TODO: Change this to official version when scala-pact supports basic auth and tag encoding
// https://github.com/ITV/scala-pact/pull/114
// https://github.com/ITV/scala-pact/pull/115
addSbtPlugin("com.itv" % "sbt-scalapact" % "2.3.4-NDLA-1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")
