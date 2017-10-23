# ARTICLE API 
[![Build Status](https://travis-ci.org/NDLANO/article-api.svg?branch=master)](https://travis-ci.org/NDLANO/article-api)

## Usage
Creates, updates and returns an `Article`. Implements Elasticsearch for search within the article database.

To interact with the api, you need valid security credentials; see [Access Tokens usage](https://github.com/NDLANO/auth/blob/master/README.md).
To write data to the api, you need write role access.

It also has as internal import routines for importing data from the old system to this database. There are a number of cleaning and
reporting services pertaining to the import which are only available for internal admin services. 

## Article format
The endpoint `GET /article-api/v2/articles/<id>` will fetch a json-object containing the article in the selected language.
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

### Validation of article content

Whenever an article is created or updated a validation of the content itself is performed. This is to ensure that only accepted tags and attributes are
used.
A list of permitted HTML/MathML tags and attributes are specified in `src/main/resources/html-rules.json` and `src/main/resources/mathml-rules`.json.
The `tags` section defines permitted tags without attributes. The `attributes` section defines tags with a list of permitted attributes.

Extra validation is performed on the `<embed>` tag: based on the `data-resource` attribute a different set of **required** attributes must also be present.
These rules are defined in `src/main/resources/embed-tag-rules.json`. Should any attribute other than those in the required list be present,
they will be stripped before validation (this step is only performed on `<embed>` tags).

When an article is fetched with the `GET` API endpoint, the api will add a `data-url` attribute on every `<embed>` tag which also contains a `data-resource_id` attribute.
A `data-id` attribute is appended to each `<embed>` tag.

### Resource types for embed tags
The embed tag contains a set of attributes which define what content should be inserted. The list below provides an explaination of each recognized attribute.

#### Required Attributes

* **data-resource** - defines the type of resource that should be inserted (can be image, audio, link to another article, ...). Present in every embed tag
* **data-id** - a unique number identifying the embed tag in an article. Present in every embed tag
* **data-url** - a url linking to the resource to insert. Present in image, audio, h5p, external, and nrk
* **data-alt** - alternative text to display if the resource could not be inserted. Present in image
* **data-size** - a hint as to how large the resource should be when presented. Present in image
* **data-align** - a hint as to the alignment of the resource when presented. Can be "left" or "right". Present in image
* **data-player** - a video player identifier used to play videos. Present in brightcove
* **data-message** - a message to be displayed when presented. Present in error
* **data-caption** - a caption to display along with the resource. Present in brightcove and image
* **data-account** - the brightcove account id. Present in brightcove
* **data-videoid** - a video identifier. Present in brightcove
* **data-link-text** - the text to display in a link. Present in content-link and concept
* **data-content-id** - the id to the article content to be inserted. Present in content-link and concept
* **data-nrk-video-id** - an ID to nrk videos. Present in nrk
* **data-resource_id** - an ID to an internal resource to be inserted. Present in image and audio

#### Optional attributes

Optional attribute groups are defined in [src/main/resources/embed-tag-rules.json](src/main/resources/embed-tag-rules.json).
Every attribute in an attribute group must be specified. Otherwise validation will fail.
The following attribute groups exists:
* `data-upper-left-x`, `data-upper-left-y`, `data-lower-right-x`, `data-lower-right-y`: Can only be used for image embed-tags
* `data-focal-x`, `data-focal-y`: Can only be used for image embed-tags


### Other tags with extra attributes
The following tags may contain extra attributes which conveys information about how they should be displayed
* `ol`
  * **data-type** - If present and with the value "letters" the bullets should be letters.
* `aside`
  * **data-type** - Specifies how the aside should be viewed

## Developer documentation

**Compile**: sbt compile

**Run tests:** sbt test

**Create Docker Image:** sbt docker

### IntegrationTest Tag and sbt run problems
Tests that need a running elasticsearch outside of component, e.g. in your local docker are marked with selfdefined java
annotation test tag  ```IntegrationTag``` in ```/ndla/article-api/src/test/java/no/ndla/tag/IntegrationTest.java```. 
As of now we have no running elasticserach or tunnel to one on Travis and need to ignore these tests there or the build will fail.  
Therefore we have the
 ```testOptions in Test += Tests.Argument("-l", "no.ndla.tag.IntegrationTest")``` in ```build.sbt```  

    sbt "test-only -- -n no.ndla.tag.IntegrationTest"

