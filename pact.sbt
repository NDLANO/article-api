import scala.sys.process._

pactBrokerAddress := sys.env("PACT_BROKER_URL")
pactBrokerCredentials := (
  sys.env("PACT_BROKER_USERNAME"),
  sys.env("PACT_BROKER_PASSWORD")
)
pactContractTags := Seq(
  sys.env.getOrElse(
    "TRAVIS_BRANCH",
    git.gitCurrentBranch.value
  ))
pactContractVersion := ("git rev-parse --short HEAD" !!).trim
