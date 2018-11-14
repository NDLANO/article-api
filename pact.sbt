pactBrokerAddress := "http://pact-broker.ndla-local"
pactContractVersion := git.gitHeadCommit.value
  .map(sha => sha.take(7))
  .get

providerName := "article-api"
consumerNames := Seq("draft-api")
