# commercetools <-> PAYONE Integration Service

[![Build Status](https://travis-ci.com/commercetools/commercetools-payone-integration.svg?token=BGS8vSNxuriRBqs9Ffzs&branch=master)](https://travis-ci.com/commercetools/commercetools-payone-integration)

This software provides an integration between the [commercetools eCommerce platform](http://dev.sphere.io) API
and the [PAYONE](http://www.payone.de) payment service provider API. 

It is a standalone Microservice that connects the two cloud platforms and provides own helper APIs to checkout
implementations. 

(remove after public:) 
 * There is a public hipchat support room for all things CT API and JVM SDK https://www.hipchat.com/gCOStFSHE
 * Same for skype: https://join.skype.com/dc9D8GW9SFnp 
 
## Resources
 * commercetools API documentation at http://dev.commercetools.com
 * commercetools JVM SDK Javadoc at http://sphereio.github.io/sphere-jvm-sdk/javadoc/master/index.html
 * commercetools general payment conventions, esp. for the payment type modeling https://github.com/nkuehn/payment-specs
 * PAYONE API documentation https://pmi.pay1.de/merchants/?navi=downloads 
 * The PSP integrations requirements and checkout protocol specification document (sent to you individually for now)
 * Documentation of the integration service http://commercetools.github.io/commercetools-payone-integration/index.html
   * including [latest "living" specification](http://commercetools.github.io/commercetools-payone-integration/latest/spec/specs/Specs.html)
 
## Using the Integration in a project

TODO

### Required Configuration in commercetools

TODO

### Required Configuration in PAYONE

https://pmi.pay1.de/

 * create a Payment Portal of type "Shop" for the site you are planning (please also maintain separate portal for 
   automated testing, demo systems etc.)
 * set the hashing algorithm to sha2-384  ("advanced" tab in the portal config)
 * put the notification listener URL of where you will deploy the microservice into "Transaction Status URL" in the 
   "advanced" tab of the portal
 * configure the "riskcheck" settings as intended (esp. 3Dsecure)

-> DO NOT USE A MERCHANT ACCOUNT ACROSS commercetools projects, you may end up mixing customer accounts (debitorenkonten). 

### Configuration Options of the Integration itself

TODO 

### Deploy and Run

TODO docker and (complete) heroku options

#### Configuration via Environment Variables

The integration service requires - _unless otherwise stated_ - the following environment variables:

##### commercetools API client credentials

Name | Content
---- | -------
`CT_PROJECT_KEY` | the project key
`CT_CLIENT_ID` | the client id
`CT_CLIENT_SECRET` | the client secret

Can be found in [Commercetools Merchant Center](https://admin.sphere.io/).

##### PAYONE API client credentials

Name | Content
---- | -------
`PAYONE_PORTAL_ID` | Payment portal ID
`PAYONE_KEY` | Payment portal key
`PAYONE_MERCHANT_ID` | Merchant account ID
`PAYONE_SUBACC_ID` | Subaccount ID

Can be found in the [PAYONE Merchant Interface](https://pmi.pay1.de/).

##### Service configuration parameters

All optional.

Name | Content | Default
---- | ------- | --------
`CRON_NOTATION` | cron expression to specify when the service will poll for commercetools messages like [PaymentInteractionAdded](http://dev.commercetools.com/http-api-projects-messages.html#payment-interaction-added-message) | poll every 30 seconds
`PAYONE_MODE` | the mode of operation with PAYONE <ul><li>`"live"` for production mode, (i.e. _actual payments_) or</li><li>`"test"` for test mode</li></ul> | `"test"`  
`CT_START_FROM_SCRATCH` | :warning: _**Handle with care!**_ If and only if equal, ignoring case, to `"true"` the service will create the custom types it needs. _**Therefor it first deletes all Order, Cart, Payment and Type entities**_. See [issue #34](https://github.com/commercetools/commercetools-payone-integration/issues/34). | `"false"`

### Notes to the checkout implementation

 1. If the PAYONE invoice generation feature or the Klarna payment methods are to be supported, the checkout has to make
    sure that 
    `amountPlanned = Sum over all Line Items ( round ( totalPrice.centAmount / quantity ) * quantity ))` 
    and handle deviations accordingly.  Deviations can especially occur if absolute discounts are applied and there are
    Line Items with quantity > 1.  On deviations the Line Item Data will not be transferred to PAYONE. 

## Test environments

SEE PAYONE DOCUMENTATION - ALL TEST DATA THERE, JUST PAYPAL REQUIRES AN OWN ACCOUNT

### Functional Tests

The executable specification (using [Concordion](http://concordion.org/)) requires the following environment variables
in addition to the [commercetools API client credentials](#commercetools-api-client-credentials):

Name | Content
---- | -------
`CT_PAYONE_INTEGRATION_URL` | the URL of the service instance under test
`TEST_DATA_VISA_CREDIT_CARD_NO_3DS` | the pseudocardpan of an unconfirmed VISA credit card
`TEST_DATA_VISA_CREDIT_CARD_3DS` | the pseudocardpan of a VISA credit card verified by 3-D Secure

To run the executable specification invoke the following command line:

```
./gradlew :functionaltests:cleanTest :functionaltests:testSpec
```

Omit `:functionaltests:cleanTest` to run the tests only if something (f.i. the specification) has changed.

### Paypal Sandbox Account

https://developer.paypal.com/docs/classic/lifecycle/ug_sandbox/

To test, you need to be logged in with the developer account and then use the Sandbox Buyer credentials in the checkout (see below). 

Developer Acccount: create your own or use an existing company internal one (CT has one just in case)

Interim Sandbox Merchant: nikolaus.kuehn+facilitator-1@commercetools.de

 * Password: CT-test$
 * API user: nikolaus.kuehn+facilitator-1_api1.commercetools.de 
 * API password: J6JN4CNFGLGFFDDP
 * API signature: ADKylXE-6VhPNHJJ24JEiclO9bIyAjsZlJJJbpv6DkQJ15W1XkKa3BqV

Interim Sanbox Buyer: nikolaus.kuehn+buyer-1@commercetools.de  
 
 * Password: CT-test$
 * (has no API access) 

## Contribute Improvements

If you want to add a useful functionality or found a bug please open an issue here to announce and discuss what you
have in mind.  Then fork the project somewhere or in GitHub and create a pull request here once you're done. 

fooBar (target group are solution implementors using the integration)

## Development Notes

Please bear in mind that this repository should be free of any IDE specific files, configurations or code. Also, the use
 of frameworks and libraries should be transparent and reasonable.

## Create a custom version

Just fork it. The MIT License allows you to do anything with the code, commercially or noncommercial. :)
