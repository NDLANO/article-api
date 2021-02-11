import scala.sys.process._

pactBrokerAddress := sys.env.getOrElse("PACT_BROKER_URL", "")
pactBrokerCredentials := (
  sys.env.getOrElse("PACT_BROKER_USERNAME", ""),
  sys.env.getOrElse("PACT_BROKER_PASSWORD", "")
)
pactContractTags := Seq(
  (for {
    head <- sys.env.get("GITHUB_HEAD_REF")
    base <- sys.env.get("GITHUB_BASE_REF")
  } yield s"$base-from-$head").getOrElse(git.gitCurrentBranch.value)
)
pactContractVersion := ("git rev-parse --short=7 HEAD" !!).trim
