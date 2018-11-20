pactBrokerAddress := sys.env.get("PACT_BROKER_URL").get
pactContractVersion := git.gitHeadCommit.value
  .map(sha => sha.take(7))
  .get
pactContractTags := Seq(
  sys.env
    .get("TRAVIS_BRANCH")
    .getOrElse(git.gitCurrentBranch.value)
)
