<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


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
    - [Run as Gradle tests](#run-as-gradle-tests)
      - [IntelliJ IDEA](#intellij-idea)
    - [Development workflow](#development-workflow)
    - [Functional Tests](#functional-tests)
      - [Heroku setup](#heroku-setup)
      - [Known Heroku issues](#known-heroku-issues)
      - [Travis setup](#travis-setup)
      - [Publishing functional tests results](#publishing-functional-tests-results)
      - [Known functional tests issues](#known-functional-tests-issues)
    - [Paypal Sandbox Accounts](#paypal-sandbox-accounts)
    - [Klarna Testing notes](#klarna-testing-notes)
  - [Appendix 1: Shell script template that sets the environment variables to run the service:](#appendix-1-shell-script-template-that-sets-the-environment-variables-to-run-the-service)
  - [Appendix 2: Shell script template that sets the environment variables to run the Integration Tests](#appendix-2-shell-script-template-that-sets-the-environment-variables-to-run-the-integration-tests)
    - [Appendix 3: Alternative configuration via properties file](#appendix-3-alternative-configuration-via-properties-file)

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
Therefore the [test definitions and results](http://commercetools.github.io/commercetools-payone-integration/) are the most precise documentation of the behavior. 
They are automatically generated, updated and published to the `gh_pages` branch of this project by the [TravisCI continuous integration](https://travis-ci.com/commercetools/commercetools-payone-integration) setup. 

## Docker images

On each push to the remote github repository, a Docker image is build by travis CI.
See [travis-build.sh](/travis-build.sh)

### Tags

Every image has the following tags:
- short git commit SHA (first 8 chars), e.g. `11be0178`
- tag containing the travis build number, e.g. `travis-17`
- `latest` (if `master` branch) or `wip-branch-name` (if not `master` branch)

https://hub.docker.com/r/sphereio/commercetools-payone-integration/tags/

### Release Tag

To create a release tag for a Docker image, a new git commit tag has to be created manually.

This will trigger a new Docker build by travis CI and will create two additional Docker tags:
- git tag value, e.g. `v1.0.1`
- `production`

The git release tag can be created via command line or github UI ([Draft new Release](https://github.com/commercetools/commercetools-payone-integration/releases))

```bash
git tag -a v1.0.1 -m "Minor text adjustments."
```
 
### Build

The Integration is built as a "fat jar" that can be directly started via  the `java -jar` command. 
The jar is built and run as follows:

```
./gradlew stage
java -jar service/build/libs/commercetools-payone-integration.jar
```

### Run

At the end of this README you can find a copy/paste shell template that sets the variables. Alternatively you can use
[configuration via properties file](#appendix-3-alternative-configuration-via-properties-file)

The integration service itself does not provide SSL connectivity, this must be done by a load balancer / SSL terminator 
running in front of it (which is recommended in any case).
 
#### Local run for development and tests

  In the [`/build.gradle`](/build.gradle) script the [_application_](https://docs.gradle.org/current/userguide/application_plugin.html)
  plugin is applied 
  thus `gradle run` task could be used for local run and debug 
  (for example, in Intellij IDEA you could _Run/Debug_ the gradle task `run` from the tasks list).
  
  In the script this `run` task is configured to convert the required runtime values from the local gradle settings 
(`gradle.properties`) to java runtime properties (`-Dkey=value` arguments). 
This allows to skip manually set-up environment variables for the service run.

  If you wish to build/run/debug the app from command line use the next commands (port `1044` is variable): 

  * Listen mode:
      ```
      java -agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=1044 -jar service/build/libs/commercetools-payone-integration.jar
      ```

  * Attach mode:
      ```
      java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044 -jar service/build/libs/commercetools-payone-integration.jar
      ```
      
**Note**: if the mandatory environment variables are not exported beforehand you should export them before `java` command,
for instance like this:
```
 source .service.sh && java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044 -jar service/build/libs/commercetools-payone-integration.jar
```
where `.service.sh` is a file with secret variables like described in [Appendix 1: Shell script template that sets the environment variables to run the service:](#appendix-1-shell-script-template-that-sets-the-environment-variables-to-run-the-service)

## Test environments

Via the Payone PMI you have access to a full set of test data, which are implemented in the integration tests of this integration. 

As a notable exception, testing PayPal payments requires developer sandbox accounts at PayPal (see [Paypal Sandbox Accounts](#paypal-sandbox-accounts)).

**Warning**: Due to PayPal's complex and restrictive browser session handling and the parallel execution of tests (necessary due to PAYONE's notifications which take up to 7 minutes per transaction)
a separate account is required for each of the transaction types (see [Functional Tests configuration](#functional-tests)).

### Run as Gradle tests

#### IntelliJ IDEA

Open **Preferences...**, go to
 
`Build, Execution, Deployment > Build Tools > Gradle > Runner`

Select **Gradle Tests Runner** in the **Run tests using** dropdown and apply the changes.

[Stack Overflow: How to run test not as a JUnit but as a Gradle test](http://stackoverflow.com/a/34333598/980828)

### Development workflow

> TODO document best practice on how to work in day-to-day development, esp. on how local machine, travis and heroku play together.  

The integration tests of this implementation use a heroku instance of the service. If you are authorized to configure 
it the backend can be found at https://dashboard.heroku.com/apps/ct-payone-integration-staging/resources .

Please do not access this instance for playground or experimental reasons as you may risk breaking running automated 
integration tests. 

### Functional Tests

You could find the values for Heroku test service and Travis functional tests in the encrypted [`travis-build/`](/travis-build) directory.

**Note**: it's important to update  [`travis-build/`](/travis-build) settings every time you change the build settings 
in Travis or Heroku config. _Even better update first the file, then Travis and Heroku settings pages_.

#### Heroku setup

The test service run in Heroku expects to have the next values:

<table>
  <tr><th>Name</th><th>Content</th></tr>
  <tr><td><code>LONG_TIME_FRAME_SCHEDULED_JOB_CRON</code></td>               <td>3 3 3 * * ? *</td></tr>
  <tr><td><code>SHORT_TIME_FRAME_SCHEDULED_JOB_CRON</code></td>              <td>0/30 * * * * ? *</td></tr>
  <tr><td><code>TENANTS</code></td>                                          <td>HEROKU_FIRST_TENANT, HEROKU_SECOND_TENANT</td>
  <tr><td><code>HEROKU_FIRST_TENANT_CT_CLIENT_ID</code></td>                 <td rowspan="6">Should be adjusted with Travis setup below</td></tr>
  <tr><td><code>HEROKU_SECOND_TENANT_CT_CLIENT_ID</code></td>                </tr>
  <tr><td><code>HEROKU_FIRST_TENANT_CT_CLIENT_SECRET</code></td>             </tr>
  <tr><td><code>HEROKU_SECOND_TENANT_CT_CLIENT_SECRET</code></td>            </tr>
  <tr><td><code>HEROKU_FIRST_TENANT_CT_PROJECT_KEY</code></td>               </tr>
  <tr><td><code>HEROKU_SECOND_TENANT_CT_PROJECT_KEY</code></td>              </tr>
  <tr><td><code>HEROKU_FIRST_TENANT_CT_START_FROM_SCRATCH</code></td>        <td>false</td></tr>
  <tr><td><code>HEROKU_SECOND_TENANT_CT_START_FROM_SCRATCH</code></td>       <td>false</td></tr>
  <tr><td><code>HEROKU_FIRST_TENANT_PAYONE_KEY</code></td>                   <td rowspan="8">Should be adjusted with Travis setup below</td></tr>
  <tr><td><code>HEROKU_SECOND_TENANT_PAYONE_KEY</code></td>                  </tr>
  <tr><td><code>HEROKU_FIRST_TENANT_PAYONE_MERCHANT_ID</code></td>           </tr>
  <tr><td><code>HEROKU_SECOND_TENANT_PAYONE_MERCHANT_ID</code></td>          </tr>
  <tr><td><code>HEROKU_FIRST_TENANT_PAYONE_PORTAL_ID</code></td>             </tr>
  <tr><td><code>HEROKU_SECOND_TENANT_PAYONE_PORTAL_ID</code></td>            </tr>
  <tr><td><code>HEROKU_FIRST_TENANT_PAYONE_SUBACC_ID</code></td>             </tr>
  <tr><td><code>HEROKU_SECOND_TENANT_PAYONE_SUBACC_ID</code></td>            </tr>
  <tr><td><code>HEROKU_FIRST_TENANT_PAYONE_MODE</code></td>                  <td>test</td></tr>
  <tr><td><code>HEROKU_SECOND_TENANT_PAYONE_MODE</code></td>                 <td>test</td></tr>
  <tr><td><code>HEROKU_FIRST_TENANT_UPDATE_ORDER_PAYMENT_STATE</code></td>   <td><b>true</b></td></tr>
  <tr><td><code>HEROKU_SECOND_TENANT_UPDATE_ORDER_PAYMENT_STATE</code></td>  <td><b>false</b></td></tr>
</table> 

**Note**: 
  * `*_UPDATE_ORDER_PAYMENT_STATE` has different values for the test tenants, 
    thus the first tenant updates order state when the second one - **should not**.
  * the Integration tests validate only short scheduled job, e.g, from `SHORT_TIME_FRAME_SCHEDULED_JOB_CRON` 
    (the test creates payments without explicit handle call, then waits 30 seconds and validates the payments are handled, 
    see `ScheduledJobFactoryTest` for more details) 

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

The executable specification (using [Concordion](http://concordion.org/)) requires the following environment variables:

<table>
  <tr><th>Name</th><th>Content</th></tr>
  <tr><td><code>TEST_DATA_CT_PAYONE_INTEGRATION_URL</code></td>  <td>the URL of the service instance under test</td></tr>
  <tr><td><code>TEST_DATA_TENANT_NAME</code></td>                <td>name of the first tenant, `HEROKU_FIRST_TENANT` like in Heroku settings</td></tr>
  <tr><td><code>TEST_DATA_VISA_CREDIT_CARD_NO_3DS</code></td>    <td>test simple VISA credit card number (don't use real credit card)</td></tr>
  <tr><td><code>TEST_DATA_VISA_CREDIT_CARD_3DS</code></td>       <td>test 3-D secured VISA credit card number (dont' use real credit card)</td></tr>
  <tr><td><code>TEST_DATA_3_DS_PASSWORD</code></td>              <td>the 3DS password of the test card. Payone Test Cards use <code>12345</code></td></tr>
  <tr><td><code>TEST_DATA_SW_BANK_TRANSFER_IBAN</code></td>      <td>the IBAN of a test bank account supporting Sofortueberweisung</td></tr>
  <tr><td><code>TEST_DATA_SW_BANK_TRANSFER_BIC</code></td>       <td>the BIC of a test bank account supporting Sofortueberweisung</td></tr>
  <tr><td><code>TEST_DATA_SW_BANK_TRANSFER_PIN</code></td>       <td rowspan="2">Sofortüberweisung test credentials to verify the payment in Selenium tests</td></tr>
  <tr><td><code>TEST_DATA_SW_BANK_TRANSFER_TAN</code></td>       </tr>
  <tr><td><code>TEST_DATA_PAYONE_MERCHANT_ID</code></td>         <td rowspan="4">Payone credentials for pseudocartpan generating. <br/>
                                                                                <b>Ensure these values are the same for Travis (local) test and Heroku deployed service!</b></td></tr>
  <tr><td><code>TEST_DATA_PAYONE_SUBACC_ID</code></td></tr>
  <tr><td><code>TEST_DATA_PAYONE_PORTAL_ID</code></td></tr>
  <tr><td><code>TEST_DATA_PAYONE_KEY</code></td></tr>
  
  <tr><td><code>TEST_DATA_CT_PROJECT_KEY</code></td>             <td rowspan="3">commercetools platform credentials<br/>
                                                                                 <b>Ensure these values are the same for Travis (local) test and Heroku deployed service!</b></td></tr>
  <tr><td><code>TEST_DATA_CT_CLIENT_ID</code></td>
  <tr><td><code>TEST_DATA_CT_CLIENT_SECRET</code></td>

  <tr><td><code>TEST_DATA_TENANT_NAME_2</code></td>              <td rowspan="8">Payone and commercetools platform settings for testing the second tenant<br/>
                                                                                 <b>Ensure these values are the same for Travis (local) test and Heroku deployed service!</b></td></tr></td></tr>
  <tr><td><code>TEST_DATA_CT_PROJECT_KEY_2</code></td>           </tr>
  <tr><td><code>TEST_DATA_CT_CLIENT_ID_2</code></td>             </tr>
  <tr><td><code>TEST_DATA_CT_CLIENT_SECRET_2</code></td>         </tr>
  <tr><td><code>TEST_DATA_PAYONE_MERCHANT_ID_2</code></td>       </tr>
  <tr><td><code>TEST_DATA_PAYONE_SUBACC_ID_2</code></td>         </tr>
  <tr><td><code>TEST_DATA_PAYONE_PORTAL_ID_2</code></td>         </tr>
  <tr><td><code>TEST_DATA_PAYONE_KEY_2</code></td>               </tr>

  <tr><td><code>GH_TOKEN</code></td>                            <td>To publish build reports</td></tr>
  <tr><td><code>DOCKER_PASSWORD</code></td>                     <td rowspan="2">To publish built docker images</td></tr>
  <tr><td><code>DOCKER_USERNAME</code></td></tr>
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
* Note that the `cardtype` request argument needs to be corresponding: `V` or `M` for _VISA_ and _Master Card_ respectively.
* Each tenant generates own pseudocardpan number for the same test card number, because the pseudocardpan numbers are 
  specific for each Payone portal.

If you have all values above set in [environment variables](#appendix-2-shell-script-template-that-sets-the-environment-variables-to-run-the-integration-tests), 
and _md5_ command is available (which is default case on Mac OS X and most of Linux distributions), 
you may copy-paste and directly execute next line (change only `<VISA_CREDIT_CARD_3DS_NUMBER>`):

```
curl --data "request=3dscheck&mid=$PAYONE_MERCHANT_ID&aid=$PAYONE_SUBACC_ID&portalid=$PAYONE_PORTAL_ID&key=$(md5 -qs $PAYONE_KEY)&mode=test&api_version=3.9&amount=2&currency=EUR&clearingtype=cc&exiturl=http://www.example.com&storecarddata=yes&cardexpiredate=2512&cardcvc2=123&cardtype=V&cardpan=<VISA_CREDIT_CARD_NUMBER>" https://api.pay1.de/post-gateway/
```

To run the integration tests locally  - invoke the following command line:

```
./gradlew :functionaltests:cleanTest :functionaltests:testSpec
```

The tests take a fairly long time to run as they have to wait for the Payone notification calls to arrive.

Omit `:functionaltests:cleanTest` to run the tests only if something (f.i. the specification) has changed.

#### Publishing functional tests results

The build results are published to [`gh-pages`](https://github.com/commercetools/commercetools-payone-integration/tree/gh-pages) branch of the repo.
Then you are able to review the tests results in [Test results page](http://commercetools.github.io/commercetools-payone-integration/).

**Note:** 
  - github pages are overridden by any build of any branch, so after sequential build of different branches only the latest results are available. 
  
  - for Travis build `githubPages.repoUri` must be in HTTPS format and `$GH_TOKEN` must be set to GitHub token 
    with push permission
  
  - we should replace the plugin with newer version, see [this issue](https://github.com/commercetools/commercetools-payone-integration/issues/135)
    for more details 

#### Known functional tests issues

  - Some tests may be waiting for payment update notification for payments which are already failed. 
    These cases should be reported and avoided (fail-fast approach).
  
  - Web-driver (selenium) tests which are navigating to web-pages (Sofortüberweisung, 3ds secure verification) may fail 
    because of wrong HTML elements names, if the service providers change html structure of their sites. These cases
    require fixed on demand.

### Paypal Sandbox Accounts

To test with Paypal, you need own Sandbox Buyer credentials via a developer account.

### Klarna Testing notes

Klarna has very strict requirements to the payment details. For testing purpose one should use
[special Klarna test credentials](https://developers.klarna.com/en/de/kpm/test-credentials).

These tests require next CTP settings:

  * `gender`, `ip` and `birthday` custom fields in _payment-INVOICE-KLARNA_ custom type
  * German language, country and prices must be active.
  * 19% German tax is active
  * `test-999-cent-code` and `test-10-percent-code` discount codes and respective discounts are active
  * products from test mock cart [KlarnaCartWithTestAccountAddress.json](/blob/master/functionaltests/src/test/resources/mocks/paymentmethods/klarna/https://github.com/commercetools/commercetools-payone-integration/blob/17da0f6ed1c4b4b1e0b6d561fe03d8cfa3c0dc38/functionaltests/src/test/resources/mocks/paymentmethods/klarna/KlarnaCartWithTestAccountAddress.json)
   are published
  
## Appendix 1: Shell script template that sets the environment variables to run the service:

(fill in the values required for your environment)

```
#!/bin/sh
export TENANTS=FIRST_TENANT
export FIRST_TENANT_PAYONE_KEY=
export FIRST_TENANT_PAYONE_MERCHANT_ID=
export FIRST_TENANT_PAYONE_PORTAL_ID=
export FIRST_TENANT_PAYONE_SUBACC_ID=
export FIRST_TENANT_PAYONE_MODE=test
export FIRST_TENANT_UPDATE_ORDER_PAYMENT_STATE=true
export FIRST_TENANT_CT_PROJECT_KEY=
export FIRST_TENANT_CT_CLIENT_ID=XXX
export FIRST_TENANT_CT_CLIENT_SECRET=
export FIRST_TENANT_CT_START_FROM_SCRATCH=false
```

## Appendix 2: Shell script template that sets the environment variables to run the Integration Tests

```
#!/bin/sh
export TEST_DATA_CT_PAYONE_INTEGRATION_URL=https://ct-payone-integration-staging.herokuapp.com

export TEST_DATA_TENANT_NAME=HEROKU_FIRST_TENANT
export TEST_DATA_VISA_CREDIT_CARD_NO_3DS=
export TEST_DATA_VISA_CREDIT_CARD_3DS=
export TEST_DATA_3_DS_PASSWORD=
export TEST_DATA_SW_BANK_TRANSFER_IBAN=
export TEST_DATA_SW_BANK_TRANSFER_BIC=
export TEST_DATA_SW_BANK_TRANSFER_PIN=
export TEST_DATA_SW_BANK_TRANSFER_TAN=

export TEST_DATA_PAYONE_MERCHANT_ID=
export TEST_DATA_PAYONE_SUBACC_ID=
export TEST_DATA_PAYONE_PORTAL_ID=
export TEST_DATA_PAYONE_KEY=
export TEST_DATA_CT_PROJECT_KEY=
export TEST_DATA_CT_CLIENT_ID=
export TEST_DATA_CT_CLIENT_SECRET=

export TEST_DATA_TENANT_NAME_2=HEROKU_SECOND_TENANT
export TEST_DATA_CT_PROJECT_KEY_2=
export TEST_DATA_CT_CLIENT_ID_2=
export TEST_DATA_CT_CLIENT_SECRET_2=
export TEST_DATA_PAYONE_MERCHANT_ID_2=
export TEST_DATA_PAYONE_SUBACC_ID_2=
export TEST_DATA_PAYONE_PORTAL_ID_2=
export TEST_DATA_PAYONE_KEY_2=
```

### Appendix 3: Alternative configuration via properties file

Instead of the shell script described above a Java properties file called `gradle.properties` can be put in the project
root directory to configure the build/test. It will be picked up by Gradle (and is ignored by Git).

Alternatively you could use common gradle properties file in users directory `~/.gradle/gradle.properties`.

```
# service config, like https://dashboard.heroku.com/apps/ct-payone-integration-staging/

TENANTS=HEROKU_FIRST_TENANT
#TENANTS=HEROKU_FIRST_TENANT, HEROKU_SECOND_TENANT
HEROKU_FIRST_TENANT_PAYONE_KEY=
HEROKU_FIRST_TENANT_PAYONE_MERCHANT_ID=
HEROKU_FIRST_TENANT_PAYONE_PORTAL_ID=
HEROKU_FIRST_TENANT_PAYONE_SUBACC_ID=
HEROKU_FIRST_TENANT_PAYONE_MODE=test
HEROKU_FIRST_TENANT_UPDATE_ORDER_PAYMENT_STATE=true
HEROKU_FIRST_TENANT_CT_PROJECT_KEY=
HEROKU_FIRST_TENANT_CT_CLIENT_ID=
HEROKU_FIRST_TENANT_CT_CLIENT_SECRET=
HEROKU_FIRST_TENANT_CT_START_FROM_SCRATCH=false

HEROKU_SECOND_TENANT_PAYONE_KEY=
HEROKU_SECOND_TENANT_PAYONE_MERCHANT_ID=
HEROKU_SECOND_TENANT_PAYONE_PORTAL_ID=
HEROKU_SECOND_TENANT_PAYONE_SUBACC_ID=
HEROKU_SECOND_TENANT_PAYONE_MODE=test
HEROKU_SECOND_TENANT_UPDATE_ORDER_PAYMENT_STATE=false
HEROKU_SECOND_TENANT_CT_PROJECT_KEY=
HEROKU_SECOND_TENANT_CT_CLIENT_ID=
HEROKU_SECOND_TENANT_CT_CLIENT_SECRET=
HEROKU_SECOND_TENANT_CT_START_FROM_SCRATCH=false

# END OF service config, like https://dashboard.heroku.com/apps/ct-payone-integration-staging/

# TRAVIS build variables

TEST_DATA_CT_PAYONE_INTEGRATION_URL=https://ct-payone-integration-staging.herokuapp.com
#TEST_DATA_CT_PAYONE_INTEGRATION_URL=http://localhost:8080

TEST_DATA_TENANT_NAME=HEROKU_FIRST_TENANT
TEST_DATA_VISA_CREDIT_CARD_NO_3DS=
TEST_DATA_VISA_CREDIT_CARD_3DS=
TEST_DATA_3_DS_PASSWORD=
TEST_DATA_SW_BANK_TRANSFER_IBAN=
TEST_DATA_SW_BANK_TRANSFER_BIC=
TEST_DATA_SW_BANK_TRANSFER_PIN=
TEST_DATA_SW_BANK_TRANSFER_TAN=

TEST_DATA_PAYONE_MERCHANT_ID=
TEST_DATA_PAYONE_SUBACC_ID=
TEST_DATA_PAYONE_PORTAL_ID=
TEST_DATA_PAYONE_KEY=
TEST_DATA_CT_PROJECT_KEY=
TEST_DATA_CT_CLIENT_ID=
TEST_DATA_CT_CLIENT_SECRET=

TEST_DATA_TENANT_NAME_2=HEROKU_SECOND_TENANT
TEST_DATA_CT_PROJECT_KEY_2=
TEST_DATA_CT_CLIENT_ID_2=
TEST_DATA_CT_CLIENT_SECRET_2=
TEST_DATA_PAYONE_MERCHANT_ID_2=
TEST_DATA_PAYONE_SUBACC_ID_2=
TEST_DATA_PAYONE_PORTAL_ID_2=
TEST_DATA_PAYONE_KEY_2=

# END OF TRAVIS build variables
```
