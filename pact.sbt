pactBrokerAddress := sys.env.get("PACT_BROKER_URL").get
pactContractVersion := git.gitHeadCommit.value
  .map(sha => sha.take(7))
  .get

providerName := "article-api"
consumerNames := Seq("draft-api")
