# ARTICLE API
[![Build Status](https://travis-ci.org/NDLANO/article-api.svg?branch=master)](https://travis-ci.org/NDLANO/article-api)

API for accessing articles from NDLA

# Building and distribution

## Compile
    sbt compile

## Run tests
    #All tests except Tagged tests
    sbt test
     
    #Tests that need a running elasticsearch outside of component, e.g. in your local docker.
    sbt "test-only -- -n no.ndla.articleapi.tag.ESIntegrationTest"

## Create Docker Image
    sbt docker
