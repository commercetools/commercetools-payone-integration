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
   automated testing, demo systmes etc.)
 * set the hashing algorithm to sha2-384  ("advanced" tab in the portal config)
 * put the notification listener URL of where you will deploy the microservice into "Transaction Status URL" in the 
   "advanced" tab of the portal
 * configure the "riskcheck" settings as intended (esp. 3Dsecure)

-> DO NOT USE A MERCHANT ACCOUNT ACROSS commercetools projects, you may end up mixing customer accounts (debitorenkonten). 

### Configuration Options of the Integration itself

TODO 

### Deploy and Run

TODO docker and heroku options

### Notes to the checkout implementation

 1. If the PAYONE invoice generation feature or the Klarna payment methods are to be supported, the checkout has to make
    sure that 
    `amountPlanned = Sum over all Line Items ( round ( totalPrice.centAmount / quantity ) * quantity ))` 
    and handle deviations accordingly.  Deviations can especially occur if absolute discounts are applied and there are
    Line Items with quantity > 1.  On deviations the Line Item Data will not be transferred to PAYONE. 

## Test environments

SEE PAYONE DOCUMENTATION - ALL TEST DATA THERE, JUST PAYPAL REQUIRES AN OWN ACCOUNT

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
