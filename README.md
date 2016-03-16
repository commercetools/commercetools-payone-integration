# commercetools <-> PAYONE Integration Service

[![Build Status](https://travis-ci.com/commercetools/commercetools-payone-integration.svg?token=BGS8vSNxuriRBqs9Ffzs&branch=master)](https://travis-ci.com/commercetools/commercetools-payone-integration)

This software provides an integration between the [commercetools eCommerce platform](http://dev.sphere.io) API
and the [PAYONE](http://www.payone.de) payment service provider API. 

It is a standalone Microservice that connects the two cloud platforms and provides own helper APIs to checkout implementations. 

## Documentation

This project is using [Concordion](http://www.concordion.org) for system tests, which is a kind of "living specification". 
Therefore the [test definitions and results](http://commercetools.github.io/commercetools-payone-integration/latest/spec/specs/Specs.html) are the most precise documentation of the behavior. 
They are automatically generated, updated and published to the `gh_pages` branch of this project by the [TravisCI continuous integration](https://travis-ci.com/commercetools/commercetools-payone-integration) setup. 

## Related Documentation

 * commercetools API documentation at http://dev.commercetools.com
 * commercetools JVM SDK Javadoc at http://sphereio.github.io/sphere-jvm-sdk/javadoc/master/index.html
 * commercetools general payment conventions, esp. for the payment type modeling https://github.com/nkuehn/payment-specs
 * PAYONE API documentation https://pmi.pay1.de/merchants/?navi=downloads 
 
## Using the Integration in a project

> TODO link to generic tutorial on how to do payments.

### Required Configuration in the commercetools project

 * This integration assumes that the [conventional custom types](https://github.com/nkuehn/payment-integration-specifications/tree/master/types) for the payment methods are available in the project. 
   If they are not found, this service automatically creates them when started. 
 * In the code that creates payments, have a good plan on how to fill the "reference" custom field. 
   It appears on the customer's account statement or the customer must put it into the transfer. 
   It also must be unique. Often the Order Number is used, but this may not always suffice. 

#### Domain Constraints 

 If the PAYONE invoice generation feature or the Klarna payment methods are to be supported, the checkout has to make  sure that 
 `amountPlanned = Sum over all Line Items ( round ( totalPrice.centAmount / quantity ) * quantity ))` and handle deviations accordingly.  
 Deviations can especially occur if absolute discounts are applied and there are Line Items with quantity > 1.  
 On deviations the Line Item Data will not be transferred to PAYONE. 

### Required Configuration in PAYONE

https://pmi.pay1.de/

 * Create a Payment Portal of type "Shop" for the site you are planning (please also maintain separate portal for 
   automated testing, demo systems etc.)
 * Set the hashing algorithm to sha2-384  ("advanced" tab in the portal config)
 * Put the notification listener URL of where you will deploy the microservice into "Transaction Status URL" in the 
   "advanced" tab of the portal. The value typically is https://{your-service-instance.example.com}/payone/notification .  
 * Configure the "riskcheck" settings as intended (esp. 3Dsecure)

> Do not use a merchant account across commercetools projects, you may end up mixing customer accounts (debitorenkonten). 

### Configuration of the Integration Service itself

The integration service requires - _unless otherwise stated_ - the following environment variables. 

At the end of this README you can find a copy/paste shell template that sets the variables. Alternatively you can use
[a properties file](#appendix-2-alternative-configuration-via-properties-file).

##### commercetools API client credentials

Name | Content
---- | -------
`CT_PROJECT_KEY` | the project key
`CT_CLIENT_ID` | the client id
`CT_CLIENT_SECRET` | the client secret

Can be found in [Commercetools Merchant Center](https://admin.sphere.io/).

##### PAYONE API client credentials

All required. 

Name | Content
---- | -------
`PAYONE_PORTAL_ID` | Payment portal ID
`PAYONE_KEY` | Payment portal key
`PAYONE_MERCHANT_ID` | Merchant account ID
`PAYONE_SUBACC_ID` | Subaccount ID

Can be found in the [PAYONE Merchant Interface](https://pmi.pay1.de/).

These credentials should not be necessary in the frontend application if all transaction initiation is done through this service. 

##### Service configuration parameters

All optional.

Name | Content | Default
---- | ------- | --------
`SHORT_TIME_FRAME_SCHEDULED_JOB_CRON` | [QUARTZ cron expression](http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger) to specify when the service will poll for commercetools messages generated in the past 10 minutes like [PaymentInteractionAdded](http://dev.commercetools.com/http-api-projects-messages.html#payment-interaction-added-message) | poll every 30 seconds
`LONG_TIME_FRAME_SCHEDULED_JOB_CRON` | [QUARTZ cron expression](http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger) to specify when the service will poll for commercetools messages generated in the past 2 days | poll every hour on 5th second
`PAYONE_MODE` | the mode of operation with PAYONE <ul><li>`"live"` for production mode, (i.e. _actual payments_) or</li><li>`"test"` for test mode</li></ul> | `"test"`  
`CT_START_FROM_SCRATCH` | :warning: _**Handle with care!**_ If and only if equal, ignoring case, to `"true"` the service will create the custom types it needs. _**Therefor it first deletes all Order, Cart, Payment and Type entities**_. If not yet in the project, the Custom Types are created independently of this parameter (but only deleted and recreated if this parameter is set).  Related: [issue #34](https://github.com/commercetools/commercetools-payone-integration/issues/34). | `"false"`

### Build

The Integration is built as a "fat jar" that can be directly started via  the `java -jar` command. The jar is built as follows:

```
./gradlew stage
```

Run the JAR:

```
java -jar service/build/libs/commercetools-payone-integration.jar
```

### Deploy and Run

TODO docker

TODO availability of the /payone/notification URL to the public or just the payone servers.

The integration service itself does not provide SSL connectivity, this must be done by a load balancer / SSL terminator running in front of it (which is recommended in any case). 

## Test environments

Via the Payone PMI you have access to a full set of test data, which are implemented in the integration tests of this integration. 

As a notable exception, testing PayPal payments requires developer sandbox accounts at PayPal (see [Paypal Sandbox Accounts](#paypal-sandbox-accounts)).

:warning: Due to PayPal's complex and restrictive browser session handling and the parallel execution of tests (necessary due to PAYONE's notifications which take up to 7 minutes per transaction)
a seperate account is required for each of the transaction types (see [Functional Tests configuration](#functional-tests)).

### Development workflow

> TODO document best practice on how to work in day-to-day development, esp. on how local machine, travis and heroku play together.  

The integration tests of this implementation use a heroku instance of the service. If you are authorized to configure it the backend can be found at https://dashboard.heroku.com/apps/ct-p1-integration-staging/resources .

Please do not access this instance for playground or experimental reasons as you may risk breaking running automated integration tests. 

### Functional Tests

The executable specification (using [Concordion](http://concordion.org/)) requires the following environment variables
in addition to the [commercetools API client credentials](#commercetools-api-client-credentials):

Name | Content
---- | -------
`CT_PAYONE_INTEGRATION_URL` | the URL of the service instance under test
`TEST_DATA_VISA_CREDIT_CARD_NO_3DS` | the pseudocardpan of an unconfirmed VISA credit card
`TEST_DATA_VISA_CREDIT_CARD_3DS` | the pseudocardpan of a VISA credit card verified by 3-D Secure
`TEST_DATA_3_DS_PASSWORD` | the 3DS password of the test card. Payone Test Cards use `12345`
`TEST_DATA_SW_BANK_TRANSFER_IBAN` | the IBAN of a test bank account supporting Sofortueberweisung
`TEST_DATA_SW_BANK_TRANSFER_BIC` | the BIC of a test bank account supporting Sofortueberweisung

> TODO document how to practically acquire the pseudocardpans (from the client API). Can this be automated?
> TODO why does the 3DS pwd need an evironment variable if a fixed value? --> is a parameter which could change in future

To run the executable specification invoke the following command line:

```
./gradlew :functionaltests:cleanTest :functionaltests:testSpec
```

The tests take a fairly long time to run as they have to wait for the Payone notification calls to arrive.

Omit `:functionaltests:cleanTest` to run the tests only if something (f.i. the specification) has changed.

### Paypal Sandbox Accounts

To test with Paypal, you need Sandbox Buyer credentials.

For the time being, the following sandbox buyers are used
- for Paypal Authorization
 * email: nikolaus.kuehn+buyer-1@commercetools.de  
 * password: CT-test$
- for Paypal ChargeImmediately
 * email: zukfiprz@boximail.com
 * password: CT-test$

## Contribute Improvements

If you want to add a useful functionality or found a bug please open an issue here to announce and discuss what you
have in mind.  Then fork the project somewhere or in GitHub and create a pull request here once you're done. 

## Development Notes

Please bear in mind that this repository should be free of any IDE specific files, configurations or code. Also, the use
 of frameworks and libraries should be transparent and reasonable.

## Create a custom version

Just fork it. The MIT License allows you to do anything with the code, commercially or noncommercial. 
Contributing an Improvement is the better Idea though because you will save maintanance work when not forking. 

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
TEST_DATA_SW_BANK_TRANSFER_IBAN=""
TEST_DATA_SW_BANK_TRANSFER_BIC=""
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
