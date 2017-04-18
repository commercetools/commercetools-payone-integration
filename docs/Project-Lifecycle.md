<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Project lifecycle](#project-lifecycle)
  - [Development Notes](#development-notes)
    - [Create a custom version](#create-a-custom-version)
    - [Contribute Improvements](#contribute-improvements)
  - [Documentation](#documentation)
  - [Docker images](#docker-images)
    - [Tags](#tags)
    - [Release Tag](#release-tag)
    - [Build](#build)
    - [Run](#run)
      - [Local run for development and tests](#local-run-for-development-and-tests)
  - [Test environments](#test-environments)
    - [Development workflow](#development-workflow)
    - [Functional Tests](#functional-tests)
      - [Heroku setup](#heroku-setup)
      - [Known Heroku issues](#known-heroku-issues)
      - [Travis setup](#travis-setup)
      - [Publishing functional tests results](#publishing-functional-tests-results)
      - [Known functional tests issues](#known-functional-tests-issues)
    - [Paypal Sandbox Accounts](#paypal-sandbox-accounts)
  - [Appendix 1: Shell script template that sets the environment variables](#appendix-1-shell-script-template-that-sets-the-environment-variables)
  - [Appendix 2: Alternative configuration via properties file](#appendix-2-alternative-configuration-via-properties-file)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Project lifecycle

This document demonstrates how to import, setup, build, run, debug, test and deploy the service application.

## Development Notes

Please bear in mind that this repository should be free of any IDE specific files, configurations or code. Also, the use
 of frameworks and libraries should be transparent and reasonable.

### Create a custom version

Just fork it. The MIT License allows you to do anything with the code, commercially or noncommercial. 
Contributing an Improvement is the better Idea though because you will save maintanance work when not forking.
 
### Contribute Improvements

If you want to add a useful functionality or found a bug please open an issue here to announce and discuss what you
have in mind.  Then fork the project somewhere or in GitHub and create a pull request here once you're done. 

## Documentation

This project is using [Concordion](http://www.concordion.org) for system tests, which is a kind of "living specification". 
Therefore the [test definitions and results](http://commercetools.github.io/commercetools-payone-integration/latest/spec/specs/Specs.html) are the most precise documentation of the behavior. 
They are automatically generated, updated and published to the `gh_pages` branch of this project by the [TravisCI continuous integration](https://travis-ci.com/commercetools/commercetools-payone-integration) setup. 

## Docker images

On each push to the remote github repository, a Docker image is build by travis CI.

### Tags

Every image has the following tags:
- short git commit SHA (first 8 chars), e.g. `11be0178`
- tag containing the travis build number, e.g. `travis-17`
- `latest` (if `master` branch) or `branch-name` (if not `master` branch)

https://hub.docker.com/r/sphereio/commercetools-payone-integration/tags/

### Release Tag

To create a release tag for a Docker image, a new git commit tag has to be created manually.

This will trigger a new Docker build by travis CI and will create two additional Docker tags:
- git tag value, e.g. `v1.0.1`
- `production`

The git release tag can be created via command line or github UI ("Draft new Release" https://github.com/commercetools/commercetools-payone-integration/releases)

```bash
git tag -a v1.0.1 -m "Minor text adjustments."
```
 
**Note**: for integration tests `UPDATE_ORDER_PAYMENT_STATE` is expected to be **`true`**, 
so explicitly setup it in the Heroku environment.

### Build

The Integration is built as a "fat jar" that can be directly started via  the `java -jar` command. The jar is built as follows:

```
./gradlew stage
```

Run the JAR:

```
java -jar service/build/libs/commercetools-payone-integration.jar
```

### Run

At the end of this README you can find a copy/paste shell template that sets the variables. Alternatively you can use
[a properties file](#appendix-2-alternative-configuration-via-properties-file).

The integration service itself does not provide SSL connectivity, this must be done by a load balancer / SSL terminator 
running in front of it (which is recommended in any case).
 
#### Local run for development and tests

  In the [`/build.gradle`](/build.gradle) script the [_application_](https://docs.gradle.org/current/userguide/application_plugin.html)
  plugin is applied 
  thus `gradle run` task could be used for local run and debug 
  (for example, in Intellij IDEA you could _Run/Debug_ the gradle task `run` from the tasks list).
  
  In the script this `run` task is configured to convert the required runtime values from the local gradle settings 
(`~/.gradle/gradle.properties`) to java runtime properties (`-Dkey=value` arguments). 
This allows to skip manually set-up environment variables for the service run.

  To run the built jar from command line in debug mode use (port `1044` is variable): 

  * Listen mode:
      ```
      java -agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=1044 -jar service/build/libs/commercetools-payone-integration.jar
      ```

  * Attach mode:
      ```
      java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044 -jar service/build/libs/commercetools-payone-integration.jar
      ```

## Test environments

Via the Payone PMI you have access to a full set of test data, which are implemented in the integration tests of this integration. 

As a notable exception, testing PayPal payments requires developer sandbox accounts at PayPal (see [Paypal Sandbox Accounts](#paypal-sandbox-accounts)).

:warning: Due to PayPal's complex and restrictive browser session handling and the parallel execution of tests (necessary due to PAYONE's notifications which take up to 7 minutes per transaction)
a seperate account is required for each of the transaction types (see [Functional Tests configuration](#functional-tests)).

### Development workflow

> TODO document best practice on how to work in day-to-day development, esp. on how local machine, travis and heroku play together.  

The integration tests of this implementation use a heroku instance of the service. If you are authorized to configure 
it the backend can be found at https://dashboard.heroku.com/apps/ct-payone-integration-staging/resources .

Please do not access this instance for playground or experimental reasons as you may risk breaking running automated 
integration tests. 

### Functional Tests

You could find the values for Heroku test service and Travis functional tests in the encrypted [`travis-build/`](/travis-build) directory.

**Note**: it's important to update  [`travis-build/`](/travis-build) settings every time you change the build settings 
in Travis config. Even better update first the file, then on the Travis web page.

#### Heroku setup

The test service run it Heroku expected to have the next values:

<table>
  <tr><th>Name</th><th>Content</th></tr>
  <tr><td><code>CT_CLIENT_ID</code></td>                <td rowspan="3">Should be adjusted with Travis setup below</td></tr>
  <tr><td><code>CT_CLIENT_SECRET</code></td>            </tr>
  <tr><td><code>CT_PROJECT_KEY</code></td>              </tr>
  <tr><td><code>CT_START_FROM_SCRATCH</code></td>       <td>false</td></tr>
  <tr><td><code>PAYONE_KEY</code></td>                  <td rowspan="4">Should be adjusted with Travis setup below</td></tr>
  <tr><td><code>PAYONE_MERCHANT_ID</code></td>          </tr>
  <tr><td><code>PAYONE_PORTAL_ID</code></td>            </tr>
  <tr><td><code>PAYONE_SUBACC_ID</code></td>            </tr>
  <tr><td><code>PAYONE_MODE</code></td>                 <td>test</td></tr>
  <tr><td><code>UPDATE_ORDER_PAYMENT_STATE</code></td>  <td>true</td></tr>
</table> 

Scheduler values are optional: this feature is not covered by the tests.

#### Known Heroku issues

  Some times Heroku environment has problems, so the functional tests might be unstable. To check Heroku issues use:

  - Heroku CLI commands:
    - `heroku apps:errors`
    - `heroku status`
    - `heroku logs --tail`

  - Heroku web-page https://status.heroku.com/

  As an example of the potential issues see ["Increased rate of "H10 App Crashed" errors"](https://status.heroku.com/incidents/1091)
  incident report, which caused  inconsistent _Socket Timeout_, _503 Service unavailable_ and other connection errors.

#### Travis setup

The executable specification (using [Concordion](http://concordion.org/)) requires the following environment variables
in addition to the [commercetools API client credentials](#commercetools-api-client-credentials):

<table>
  <tr><th>Name</th><th>Content</th></tr>
  <tr><td><code>CT_PAYONE_INTEGRATION_URL</code></td>           <td>the URL of the service instance under test</td></tr>
  <tr><td><code>TEST_DATA_VISA_CREDIT_CARD_NO_3DS</code></td>   <td>test simple VISA credit card number (don't use real credit card)</td></tr>
  <tr><td><code>TEST_DATA_VISA_CREDIT_CARD_3DS</code></td>      <td>test 3-D secured VISA credit card number (dont' use real credit card)</td></tr>
  <tr><td><code>TEST_DATA_3_DS_PASSWORD</code></td>             <td>the 3DS password of the test card. Payone Test Cards use <code>12345</code></td></tr>
  <tr><td><code>TEST_DATA_SW_BANK_TRANSFER_IBAN</code></td>     <td>the IBAN of a test bank account supporting Sofortueberweisung</td></tr>
  <tr><td><code>TEST_DATA_SW_BANK_TRANSFER_BIC</code></td>      <td>the BIC of a test bank account supporting Sofortueberweisung</td></tr>
  <tr><td><code>TEST_DATA_PAYONE_MERCHANT_ID</code></td>        <td rowspan="4">Payone credentials for pseudocartpan generating. <br/>
                                                                                <b>Ensure these values are the same for Travis (local) test and Heroku deployed service!</b></td></tr>
  <tr><td><code>TEST_DATA_PAYONE_SUBACC_ID</code></td></tr>
  <tr><td><code>TEST_DATA_PAYONE_PORTAL_ID</code></td></tr>
  <tr><td><code>TEST_DATA_PAYONE_KEY</code></td></tr>
</table> 

The pseudocardpan for VISA is fetched at runtime for specified `TEST_DATA_VISA_CREDIT_CARD_NO_3DS`/`TEST_DATA_VISA_CREDIT_CARD_3DS`
card numbers using PAYONE server API. To do this with the client API please refer to the corresponding Payone API documentation.
With the server API you simply need to send a POST request of type "3dscheck" for example by using a command line tool:

```
curl --data "request=3dscheck&mid=<PAYONE_MERCHANT_ID>&aid=<PAYONE_SUBACC_ID>&portalid=<PAYONE_PORTAL_ID>&key=<MD5_PAYONE_KEY>&mode=test&api_version=3.9&amount=2&currency=EUR&clearingtype=cc&exiturl=http://www.example.com&storecarddata=yes&cardexpiredate=2512&cardcvc2=123&cardtype=V&cardpan=<VISA_CREDIT_CARD_NUMBER>" https://api.pay1.de/post-gateway/
```

* <url> needs to be replaced by the PAYONE api url. You will find this in the server documentation.
* You need to replace the values for mid, aid and portalid with the ones you want to use with PAYONE.
* The value for `MD5_PAYONE_KEY` needs to be the MD5 encryption result of your PAYONE key (`PAYONE_KEY`). 
Use `md5 -qs $PAYONE_KEY` to hash string value to MD5.
* The `cardpan` must be the value of card number. You may get test data values from [wiki page]( https://wiki.commercetools.de/display/DEV/payone#payone-Creditcard%28canbetestedwithpublicIPonly%29).
* Note that the `cardtype` request argument needs to be correspondand: `V` or `M` for _VISA_ and _Master Card_ respectively.

If you have all values above set in [environment variables](#appendix-2-alternative-configuration-via-properties-file), 
and _md5_ command is available (which is default case on Mac OS X and most of Linux distributions), 
you may copy-paste and directly execute next line (change only `<VISA_CREDIT_CARD_3DS_NUMBER>`):

```
curl --data "request=3dscheck&mid=$PAYONE_MERCHANT_ID&aid=$PAYONE_SUBACC_ID&portalid=$PAYONE_PORTAL_ID&key=$(md5 -qs $PAYONE_KEY)&mode=test&api_version=3.9&amount=2&currency=EUR&clearingtype=cc&exiturl=http://www.example.com&storecarddata=yes&cardexpiredate=2512&cardcvc2=123&cardtype=V&cardpan=<VISA_CREDIT_CARD_NUMBER>" https://api.pay1.de/post-gateway/
```

> NOTE:  When sending "storecarddata=yes" at the end you will receive the pseudocardpan in the response from PAYONE.

To run the executable specification invoke the following command line:

```
./gradlew :functionaltests:cleanTest :functionaltests:testSpec
```

The tests take a fairly long time to run as they have to wait for the Payone notification calls to arrive.

Omit `:functionaltests:cleanTest` to run the tests only if something (f.i. the specification) has changed.

#### Publishing functional tests results

The build results are published to [`gh-pages`](https://github.com/commercetools/commercetools-payone-integration/tree/gh-pages) branch of the repo.
Then you are able to review the tests results in [Test results page](http://commercetools.github.io/commercetools-payone-integration/).

**Note:** 
  - github pages are overridden by any build of any branch, so in 
  
  - for Travis build `githubPages.repoUri` must be in HTTPS format and `$GH_TOKEN` must be set to GitHub token 
    with push permission
  
  - we should replace the plugin with newer version, see [this issue](https://github.com/commercetools/commercetools-payone-integration/issues/135)
    for more details 

#### Known functional tests issues

  - Some tests are waiting for payment update notification for payments which are already failed. 
    These cases should be reported and avoided (fail-fast approach).
  
  - Web-driver (selenium) tests which are navigating to web-pages (Sofortüberweisung, 3ds secure verification) may fail 
    because of wrong HTML elements names, if the service providers change html structure of their sites.

### Paypal Sandbox Accounts

To test with Paypal, you need own Sandbox Buyer credentials via a developer account. Available from commercetools, too; please contact support. 

## Appendix 1: Shell script template that sets the environment variables

(fill in the values required for your environment)

```
#!/bin/sh
export CT_PROJECT_KEY=""
export CT_CLIENT_ID=""
export CT_CLIENT_SECRET=""
export CT_START_FROM_SCRATCH="false"

export PAYONE_KEY=""
export PAYONE_MERCHANT_ID=""
export PAYONE_MODE=""
export PAYONE_PORTAL_ID=""
export PAYONE_SUBACC_ID=""

# from here on only test related

export CT_PAYONE_INTEGRATION_URL=""

export TEST_DATA_VISA_CREDIT_CARD_NO_3DS=""
export TEST_DATA_VISA_CREDIT_CARD_3DS=""
export TEST_DATA_3_DS_PASSWORD=""
export TEST_DATA_SW_BANK_TRANSFER_IBAN=""
export TEST_DATA_SW_BANK_TRANSFER_BIC=""
```

## Appendix 2: Alternative configuration via properties file

Instead of the shell script described above a Java properties file called `gradle.properties` can be put in the project
root directory to configure the build/test. It will be picked up by Gradle (and is ignored by Git).

```
# use the Gradle daemon, i.e. re-use the JVM for builds
org.gradle.daemon=true

# integration service configuration

CT_PROJECT_KEY=<commercetools project key>
CT_CLIENT_ID=<commercetools client ID>
CT_CLIENT_SECRET=<commercetools client secret>

PAYONE_KEY=<PAYONE Key>
PAYONE_MERCHANT_ID=<PAYONE merchant ID>
PAYONE_MODE=<PAYONE mode (live or test)>
PAYONE_PORTAL_ID=<PAYONE portal ID>
PAYONE_SUBACC_ID=<PAYONE subaccount>

# test configuration

CT_PAYONE_INTEGRATION_URL=<URL of the integration service instance under test>

TEST_DATA_VISA_CREDIT_CARD_NO_3DS=<see PAYONE Test data documentation>
TEST_DATA_VISA_CREDIT_CARD_3DS=<see PAYONE Test data documentation>
TEST_DATA_3_DS_PASSWORD=<see PAYONE Test data documentation>
TEST_DATA_SW_BANK_TRANSFER_IBAN=<see PAYONE Test data documentation>
TEST_DATA_SW_BANK_TRANSFER_BIC=<see PAYONE Test data documentation>
```
