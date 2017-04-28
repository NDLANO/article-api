# ARTICLE API 
[![Build Status](https://travis-ci.org/NDLANO/article-api.svg?branch=master)](https://travis-ci.org/NDLANO/article-api)

API for accessing articles from NDLA

# Usage
Creates, updates and returns an `Article`. Implements Elasticsearch for search within the article database.

To interact with the api, you need valid security credentials; see [Access Tokens usage](https://github.com/NDLANO/auth/blob/master/README.md).
To write data to the api, you need write role access.

It also has as internal import routines for importing data from the old system to this database. There are a number of cleaning and
reporting services pertaining to the import which are only available for internal admin services. 

# Article format
The endpoint `GET /article-api/v1/articles/<id>` will fetch a json-object containing the article in all languages it is translated to.
The article body contained in this json-object consists of a strict subset of permitted HTML tags. It may also contain a special tag, `<embed>`,
which is used to refer to content located in other APIs (including, but not limited to images, audio, video and H5P).

Some example usages of the embed tag:
* `<embed data-resource="external" data-url="https://youtu.be/7ZVyNjKSr0M" data-id="0" />` refers content from an external source (data-resource). In this case Youtube.
  `data-url` is the url where the resource can be found, `data-id` is an embed tag-identifier used to identify unique embeds in this article and is located in every embed tag.
* `<embed data-align="" data-alt="Fyrlykt med hav og horisont" data-caption="" data-resource="image" data-size="fullbredde" data-id="1" data-url="http://image-api-url/image-api/v1/images/179" />`
  refers to an image (`data-resource`) located in the [image API](https://github.com/NDLANO/image-api), along with alignment info (`data-align`), alternative text (`data-alt`),
  image caption (`data-caption`), intended display size in the article (`data-size`), the unique embed tag-identifier (`data-id`) and the url where image metadata can be found (`data-url`).

In order to display the article in its intended form all these embed-tags needs to be parsed and replaced with the content from its respectable source.
The [article converter](https://github.com/NDLANO/article-converter) implements the logic needed to do this.


For a more detailed documentation of the API, please refer to the [API documentation](https://api.ndla.no) (Staging: [API documentation](https://staging.api.ndla.no)).

# Developer documentation

**Compile**: sbt compile

**Run tests:** sbt test

**Create Docker Image:** sbt docker

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

