# CONTENT API 
API for accessing content from NDLA

# Building and distribution

## Compile
    sbt compile

## Run tests
    sbt test

## Publish to nexus
    sbt publish

## Create Docker Image
    sbt docker

You need to have a docker daemon running locally. Ex: [boot2docker](http://boot2docker.io/)

## Deploy Docker Image to Amazon (via DockerHub)
    ndla deploy <env> content-api

