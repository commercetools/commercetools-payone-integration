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
    - [Run as Gradle tests](#run-as-gradle-tests)
      - [IntelliJ IDEA](#intellij-idea)
    - [Development workflow](#development-workflow)
    - [Paypal Sandbox Accounts](#paypal-sandbox-accounts)
    - [Klarna Testing notes](#klarna-testing-notes)
  - [Appendix 1: Shell script template that sets the environment variables to run the service:](#appendix-1-shell-script-template-that-sets-the-environment-variables-to-run-the-service)
  - [Appendix 2: Alternative configuration via properties file](#appendix-2-alternative-configuration-via-properties-file)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Project lifecycle

This document demonstrates how to import, setup, build, run, debug, test and deploy the service application.

## Development Notes

Please bear in mind that this repository should be free of any IDE specific files, configurations or code. Also, the use
 of frameworks and libraries should be transparent and reasonable.

### Create a custom version

Just fork it. The MIT License allows you to do anything with the code, commercially or noncommercial. 
Contributing an Improvement is the better Idea though because you will save maintenance work when not forking.
 
### Contribute Improvements

If you want to add a useful functionality or found a bug please open an issue here to announce and discuss what you
have in mind. Then fork the project somewhere or in GitHub and create a pull request here once you're done.

## Documentation

The [test definitions and results](http://commercetools.github.io/commercetools-payone-integration/) are the most precise documentation of the behavior. 
They are automatically generated, updated and published to the `gh_pages` branch of this project by the [TravisCI continuous integration](https://travis-ci.com/commercetools/commercetools-payone-integration) setup.

## Docker images

On each push to the remote github repository, a Docker image is build by travis CI.
See [travis-build.sh](/travis-build.sh)

### Tags

Every image has the following tags:
- short git commit SHA (first 8 chars), e.g. `11be0178`
- tag containing the travis build number, e.g. `travis-17`
- `latest` (if `master` branch) or `wip-branch-name` (if not `master` branch)

https://hub.docker.com/r/commercetools/commercetools-payone-integration/tags/

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
[configuration via properties file](#appendix-2-alternative-configuration-via-properties-file)

The integration service itself does not provide SSL connectivity, this must be done by a load balancer / SSL terminator 
running in front of it (which is recommended in any case).
 
#### Local run for development and tests

  In the [`/build.gradle`](/build.gradle) script the [_application_](https://docs.gradle.org/current/userguide/application_plugin.html)
  plugin is applied 
  thus `./gradlew run` or `./gradlew :service:run` task could be used for local run and debug 
  (for example, in Intellij IDEA you could _Run/Debug_ the gradle task `run` from the tasks list: 
  `commercetools-payone-integration -> :service -> Tasks -> application -> run`).
  
  In the script this `run` task is configured to convert the required runtime values from the local gradle settings 
(`gradle.properties`) to java runtime properties (`-Dkey=value` arguments). 
This allows to skip manually set-up environment variables for the service run.

  Also, with included [_shadow jar gradle plugin_](http://imperceptiblethoughts.com/shadow/) 
  one could run/debug the application using shadowed (fat/über) jar using `./gradlew runShadow` (`./gradlew :service:runShadow`) task. 
  Opposite to simple `run` this will build full jar with all included metadata (like project name, version and so on).
  This could be useful if one wants to test `/health` resource to verify gradle built-in name/version resolving.

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

### Run as Gradle tests

#### IntelliJ IDEA

Open **Preferences...**, go to
 
`Build, Execution, Deployment > Build Tools > Gradle > Runner`

Select **Gradle Tests Runner** in the **Run tests using** dropdown and apply the changes.

[Stack Overflow: How to run test not as a JUnit but as a Gradle test](http://stackoverflow.com/a/34333598/980828)

### Test Integration workflow

To run integration workflow and test the payment transactions with Payone, You need to clone `test.internal.properties.skeleton` file, rename it to `test.internal.properties` and fill in the values for the properties, or you can also provide the below environment variables while running the tests. 

```
TENANTS=FIRST_TENANT
TEST_DATA_CT_PROJECT_KEY=
TEST_DATA_CT_CLIENT_ID=
TEST_DATA_CT_CLIENT_SECRET=
TEST_DATA_PAYONE_KEY=
TEST_DATA_PAYONE_MERCHANT_ID=
TEST_DATA_PAYONE_PORTAL_ID=
TEST_DATA_PAYONE_SUBACC_ID=
```

See [test.internal.properties.skeleton](https://github.com/commercetools/commercetools-payone-integration/blob/it-workflow/service/src/test/resources/test.internal.properties.skeleton) for more details. 

### Development workflow

> TODO document best practice on how to work in day-to-day development, esp. on how local machine, travis and heroku play together.

### Paypal Sandbox Accounts

To test with Paypal, you need own Sandbox Buyer credentials via a developer account.

### Klarna Testing notes

Klarna has very strict requirements to the payment details. For testing purpose one should use
[special Klarna test credentials](https://developers.klarna.com/en/de/kpm/test-credentials).

These tests require next CTP settings:

  * `gender`, `ip` and `birthday` custom fields in _payment-INVOICE-KLARNA_ custom type
  * German language, country and prices must be added/enabled in testing CTP project/products.
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

## Appendix 2: Alternative configuration via properties file

Instead of the shell script described above a Java properties file called `gradle.properties` can be put in the project
root directory to configure the build/test. It will be picked up by Gradle (and is ignored by Git).

Alternatively you could use common gradle properties file in users directory `~/.gradle/gradle.properties`.

```
# service config, like https://dashboard.heroku.com/apps/ct-payone-integration-staging/

TENANTS=FIRST_TENANT
#TENANTS=FIRST_TENANT, SECOND_TENANT
FIRST_TENANT_PAYONE_KEY=
FIRST_TENANT_PAYONE_MERCHANT_ID=
FIRST_TENANT_PAYONE_PORTAL_ID=
FIRST_TENANT_PAYONE_SUBACC_ID=
FIRST_TENANT_PAYONE_MODE=test
FIRST_TENANT_UPDATE_ORDER_PAYMENT_STATE=true
FIRST_TENANT_CT_PROJECT_KEY=
FIRST_TENANT_CT_CLIENT_ID=
FIRST_TENANT_CT_CLIENT_SECRET=
FIRST_TENANT_CT_START_FROM_SCRATCH=false

SECOND_TENANT_PAYONE_KEY=
SECOND_TENANT_PAYONE_MERCHANT_ID=
SECOND_TENANT_PAYONE_PORTAL_ID=
SECOND_TENANT_PAYONE_SUBACC_ID=
SECOND_TENANT_PAYONE_MODE=test
SECOND_TENANT_UPDATE_ORDER_PAYMENT_STATE=false
SECOND_TENANT_CT_PROJECT_KEY=
SECOND_TENANT_CT_CLIENT_ID=
SECOND_TENANT_CT_CLIENT_SECRET=
SECOND_TENANT_CT_START_FROM_SCRATCH=false

# END OF service config, like https://dashboard.heroku.com/apps/ct-payone-integration-staging/
```
