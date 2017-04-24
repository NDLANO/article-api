# ARTICLE API 
[![Build Status](https://travis-ci.org/NDLANO/article-api.svg?branch=master)](https://travis-ci.org/NDLANO/article-api)

API for accessing articles from NDLA

# Usage
Creates, updates and returns an ```Article```. Implements elastic serach for seach within the article database.

To interact with the api, you need valid security credentials; see [Access Tokens usage](https://github.com/NDLANO/auth/blob/master/README.md).
To write data to the api, you need write role access.

Has internal import routines for importing data from the old system to this database. There are a number of cleaning and 
reporting services pertaining to the import which are only available for internal admin services. 


# Building and distribution

## Compile
    sbt compile

## Run tests
    #All tests except Tagged tests
    sbt test

### IntegrationTest Tag and sbt run problems
Tests that need a running elasticsearch outside of component, e.g. in your local docker are marked with selfdefined java
annotation test tag  ```IntegrationTag``` in ```/ndla/article-api/src/test/java/no/ndla/tag/IntegrationTest.java```. 
As of now we have no running elasticserach or tunnel to one on Travis and need to ignore these tests there or the build will fail.  
Therefore we have the
 ```testOptions in Test += Tests.Argument("-l", "no.ndla.tag.IntegrationTest")``` in ```build.sbt```  
This, it seems, will unfortunalty override runs on your local commandline so that ```sbt "test-only -- -n no.ndla.tag.IntegrationTest"```
 will not run unless this line gets commented out or you comment out the ```@IntegrationTest``` annotation in ```SearchServiceTest.scala```
 This should be solved better!

    sbt "test-only -- -n no.ndla.tag.IntegrationTest"

## Create Docker Image
    sbt docker

