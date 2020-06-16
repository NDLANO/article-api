import java.util.Properties
import com.itv.scalapact.plugin._

val Scalaversion = "2.13.1"
val Scalatraversion = "2.7.0"
val ScalaLoggingVersion = "3.9.2"
val ScalaTestVersion = "3.1.1"
val Log4JVersion = "2.11.1"
val Jettyversion = "9.4.27.v20200227"
val AwsSdkversion = "1.11.658"
val MockitoVersion = "1.11.4"
val Elastic4sVersion = "6.7.4"
val JacksonVersion = "2.10.2"
val ElasticsearchVersion = "6.8.6"
val Json4SVersion = "3.6.7"
val FlywayVersion = "5.2.0"
val PostgresVersion = "42.2.5"
val HikariConnectionPoolVersion = "3.2.0"
val TestContainersVersion = "1.12.2"

val appProperties = settingKey[Properties]("The application properties")

appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("build.properties"))
  prop
}

lazy val commonSettings = Seq(
  organization := appProperties.value.getProperty("NDLAOrganization"),
  version := appProperties.value.getProperty("NDLAComponentVersion"),
  scalaVersion := Scalaversion
)

val pactVersion = "2.3.16"

val pactTestFramework = Seq(
  "com.itv" %% "scalapact-circe-0-13" % pactVersion % "test",
  "com.itv" %% "scalapact-http4s-0-21" % pactVersion % "test",
  "com.itv" %% "scalapact-scalatest" % pactVersion % "test"
)

lazy val PactTest = config("pact") extend Test
lazy val article_api = (project in file("."))
  .configs(PactTest)
  .settings(
    inConfig(PactTest)(Defaults.testTasks),
    // Since pactTest gets its options from Test configuration, the 'Test' (default) config won't run PactProviderTests
    // To run all tests use pact config ('sbt pact:test')
    Test / testOptions := Seq(Tests.Argument("-l", "PactProviderTest")),
    PactTest / testOptions := Seq.empty
  )
  .settings(commonSettings: _*)
  .settings(
    name := "article-api",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature"),
    libraryDependencies ++= Seq(
      "ndla" %% "network" % "0.43",
      "ndla" %% "mapping" % "0.14",
      "ndla" %% "validation" % "0.41",
      "joda-time" % "joda-time" % "2.10",
      "org.scalatra" %% "scalatra" % Scalatraversion,
      "org.eclipse.jetty" % "jetty-webapp" % Jettyversion % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % Jettyversion % "container",
      "javax.servlet" % "javax.servlet-api" % "4.0.1" % "container;provided;test",
      "org.scalatra" %% "scalatra-json" % Scalatraversion,
      "org.scalatra" %% "scalatra-scalatest" % Scalatraversion % "test",
      "org.json4s" %% "json4s-native" % Json4SVersion,
      "org.scalatra" %% "scalatra-swagger" % Scalatraversion,
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,
      "org.apache.logging.log4j" % "log4j-api" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-core" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JVersion,
      "org.apache.httpcomponents" % "httpclient" % "4.5.10", // Overridden because vulnerability in request interceptor
      "org.scalikejdbc" %% "scalikejdbc" % "3.4.0",
      "org.postgresql" % "postgresql" % PostgresVersion,
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolVersion,
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkversion,
      "com.amazonaws" % "aws-java-sdk-cloudwatch" % AwsSdkversion,
      "org.scalaj" %% "scalaj-http" % "2.4.2",
      "org.elasticsearch" % "elasticsearch" % ElasticsearchVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-core" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-http" % Elastic4sVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion, // Overriding jackson-databind used in dependencies because of https://app.snyk.io/vuln/SNYK-JAVA-COMFASTERXMLJACKSONCORE-72884
      "vc.inreach.aws" % "aws-signing-request-interceptor" % "0.0.22",
      "com.google.guava" % "guava" % "28.1-jre", // Overridden because vulnerability in request interceptor
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "org.jsoup" % "jsoup" % "1.11.3",
      "log4j" % "log4j" % "1.2.16",
      "net.bull.javamelody" % "javamelody-core" % "1.74.0",
      "org.jrobin" % "jrobin" % "1.5.9", // This is needed for javamelody graphing
      "org.mockito" %% "mockito-scala" % MockitoVersion % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoVersion % "test",
      "org.flywaydb" % "flyway-core" % FlywayVersion,
      "io.lemonlabs" %% "scala-uri" % "1.5.1",
      "org.testcontainers" % "elasticsearch" % TestContainersVersion % "test",
      "org.testcontainers" % "testcontainers" % TestContainersVersion % "test",
    ) ++ pactTestFramework
  )
  .enablePlugins(DockerPlugin)
  .enablePlugins(JettyPlugin)
  .enablePlugins(ScalaPactPlugin)

assembly / assemblyJarName := "article-api.jar"
assembly / mainClass := Some("no.ndla.articleapi.JettyLauncher")
assembly / assemblyMergeStrategy := {
  case "module-info.class"                                           => MergeStrategy.discard
  case "mime.types"                                                  => MergeStrategy.filterDistinctLines
  case PathList("org", "joda", "convert", "ToString.class")          => MergeStrategy.first
  case PathList("org", "joda", "convert", "FromString.class")        => MergeStrategy.first
  case PathList("org", "joda", "time", "base", "BaseDateTime.class") => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

val checkfmt = taskKey[Boolean]("check for code style errors")
checkfmt := {
  val noErrorsInMainFiles = (Compile / scalafmtCheck).value
  val noErrorsInTestFiles = (Test / scalafmtCheck).value
  val noErrorsInBuildFiles = (Compile / scalafmtSbtCheck).value

  noErrorsInMainFiles && noErrorsInTestFiles && noErrorsInBuildFiles
}

Test / test := (Test / test).dependsOn(Test / checkfmt).value

val fmt = taskKey[Unit]("Automatically apply code style fixes")
fmt := {
  (Compile / scalafmt).value
  (Test / scalafmt).value
  (Compile / scalafmtSbt).value
}

// Make the docker task depend on the assembly task, which generates a fat JAR file
docker := (docker dependsOn assembly).value

docker / dockerfile := {
  val artifact = (assembly / assemblyOutputPath).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("adoptopenjdk/openjdk11:alpine-slim")
    run("apk", "--no-cache", "add", "ttf-dejavu")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-Dorg.scalatra.environment=production", "-jar", artifactTargetPath)
  }
}

docker / imageNames := Seq(
  ImageName(namespace = Some(organization.value),
            repository = name.value,
            tag = Some(System.getProperty("docker.tag", "SNAPSHOT")))
)

Test / parallelExecution := false

resolvers ++= scala.util.Properties
  .envOrNone("NDLA_RELEASES")
  .map(repo => "Release Sonatype Nexus Repository Manager" at repo)
  .toSeq
