# commercetools <-> PAYONE Integration Service

[![Build Status](https://travis-ci.org/commercetools/commercetools-payone-integration.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-payone-integration)

This software provides an integration between the [commercetools eCommerce platform](http://dev.commercetools.com) API
and the [PAYONE](http://www.payone.de) payment service provider (server API). 

It is a standalone Microservice that connects the two cloud platforms and provides a small own helper API to force immediate handling of a payment. 

<!-- 
     npm install -g doctoc # do it only once if not installed yet
     doctoc README.md 
     -->
<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Documentation](#documentation)
- [For developers and contributors](#for-developers-and-contributors)
- [Shop Integration Guide](#shop-integration-guide)
  - [Required Configuration in the commercetools project](#required-configuration-in-the-commercetools-project)
    - [Domain Constraints](#domain-constraints)
  - [Required Configuration in PAYONE](#required-configuration-in-payone)
  - [Configuration of the Integration Service](#configuration-of-the-integration-service)
  - [Mandatory common properties](#mandatory-common-properties)
      - [Mandatory commercetools API client credentials](#mandatory-commercetools-api-client-credentials)
      - [PAYONE API client credentials](#payone-api-client-credentials)
      - [Optional service configuration parameters](#optional-service-configuration-parameters)
    - [Docker run](#docker-run)
- [Multitenancy](#multitenancy)
  - [Scheduled tasks](#scheduled-tasks)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Documentation

  * commercetools API documentation at http://dev.commercetools.com
  * commercetools JVM SDK Javadoc at http://commercetools.github.io/commercetools-jvm-sdk/apidocs/index.html
  * commercetools general payment conventions, esp. for the payment type modeling https://github.com/nkuehn/payment-specs
  * PAYONE API documentation https://pmi.pay1.de/merchants/?navi=downloads

## For developers and contributors

Read [Project lifecycle](docs/Project-Lifecycle.md) documentation for full information.

## Shop Integration Guide

[List of actions](docs/Shop-Integration-Guide.md) which needs to be covered by front end integration.

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
 * Set the hashing algorithm to "md5 oder sha2-384 (für Migration)" ("advanced" tab in the portal config)
 * Put the notification listener URL of where you will deploy the microservice into "Transaction Status URL" in the 
   "advanced" tab of the portal. The value typically is https://{your-service-instance.example.com}/{tenant-name}/payone/notification .  
 * Configure the "riskcheck" settings as intended (esp. 3Dsecure)

> Do not use a merchant account across commercetools projects, you may end up mixing customer accounts (debitorenkonten). 

### Configuration of the Integration Service

The integration service requires - _unless otherwise stated_ - the following environment variables 
or Java runtime arguments. 
**Note**, Java runtime arguments, e.g. those supplied with `-D` prefix, have precedence over environment variables 
with the same name. 

### Mandatory common properties

Name               | Content
------------------ | -----------------
`TENANTS`          | comma or semicolon separated list of alphanumeric unique tenant names. At least one name is required. Whitespaces are ignored. Besides underscore no special characters are allowed. **Note**: provided tenant names will be used as part of handle/notification URLs. 

**Note**: the tenant names are used as a part of URI, thus use only characters allowed for path part of an URI. 
We strongly recommend not to use special or Unicode characters and limit the set with `[a-Z0-9_-]`

##### Mandatory commercetools API client credentials

All these properties must be set for every tenant name described in `TENANTS` property above.

Name                             | Content
-------------------------------- | -----------------
`TENANT1_CT_PROJECT_KEY`         | the project key
`TENANT1_CT_CLIENT_ID`           | the client id
`TENANT1_CT_CLIENT_SECRET`       | the client secret

Can be found in [commercetools Admin Center](https://admin.commercetools.com/).

##### PAYONE API client credentials

All these properties must be set for every tenant name described in `TENANTS` property above.

Name                         | Content             | Required  |
-----------------------------|---------------------|-----------|
`TENANT1_PAYONE_PORTAL_ID`   | Payment portal ID   | **Yes**   |
`TENANT1_PAYONE_KEY`         | Payment portal key  | **Yes**   |
`TENANT1_PAYONE_MERCHANT_ID` | Merchant account ID | **Yes**   |
`TENANT1_PAYONE_SUBACC_ID`   | Subaccount ID       | **Yes**   |
`TENANT1_PAYONE_MODE`        | Payment mode        | No        |

These values can be found in the [PAYONE Merchant Interface](https://pmi.pay1.de/).

##### Optional service configuration parameters

Name | Is tenant specific | Content | Default
---- | ------- | ------ | --------
`SHORT_TIME_FRAME_SCHEDULED_JOB_CRON` | No  | [QUARTZ cron expression](http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger) to specify when the service will poll for commercetools messages generated in the past 10 minutes like [PaymentTransactionAdded](http://dev.commercetools.com/http-api-projects-messages.html#paymenttransactionadded-message) and [PaymentCreated](http://dev.commercetools.com/http-api-projects-messages.html#paymentcreated-message)| poll every 30 seconds (`0/30 * * * * ? *`)
`LONG_TIME_FRAME_SCHEDULED_JOB_CRON`  | No  | same as `SHORT_TIME_FRAME_SCHEDULED_JOB_CRON`, but polls messages for the past 2 days | poll every hour on 5th second (`5 0 0/1 * * ? *`)
`TENANT1_PAYONE_MODE`                 | Yes | the mode of operation with PAYONE <ul><li>`"live"` for production mode, (i.e. _actual payments_) or</li><li>`"test"` for test mode</li></ul> | `"test"`  
`TENANT1_CT_START_FROM_SCRATCH`       | Yes | **WARNING** _**Handle with care!**_ If and only if equal, ignoring case, to `"true"` the service will create the custom types it needs. _**Therefor it first deletes all Order, Cart, Payment and Type entities**_. If not yet in the project, the Custom Types are created independently of this parameter (but only deleted and recreated if this parameter is set).  Related: [issue #34](https://github.com/commercetools/commercetools-payone-integration/issues/34). | `"false"`
`TENANT1_SECURE_KEY`                  | Yes | if provided and not empty, the value is used as the key for decrypting data from fields "IBAN" and "BIC" for payments with CustomType "PAYMENT_BANK_TRANSFER". The data must be the result of a Blowfish ECB encryption with said key and encoded in HEX. | "" (empty String)
`TENANT1_UPDATE_ORDER_PAYMENT_STATE`  | Yes | if _true_ - `Order#paymentState` will be updated when payment status notification is received from Payone. By default the order's state remains unchanged. See [Order Payment Status Mapping](/docs/Order-Payment-Status-Mapping.md) for more details. | "false" 

#### Docker run

```
docker run \
    -e TENANTS=TENANT1 
    -e TENANT1_CT_CLIENT_ID=xxx \
    -e TENANT1_CT_CLIENT_SECRET=xxx \
    -e TENANT1_CT_PROJECT_KEY=xxx \
    -e TENANT1_PAYONE_KEY=xxx \
    -e TENANT1_PAYONE_MERCHANT_ID=xxx \
    -e TENANT1_PAYONE_MODE=test|live \
    -e TENANT1_PAYONE_PORTAL_ID=xxx \
    -e TENANT1_PAYONE_SUBACC_ID=xxx \
sphereio/commercetools-payone-integration
```

## Multitenancy

Starting from version 2 a single service instance may be used for multiple tenants, e.g. same service for different shop,
merchants or suppliers. The next configuration properties are required for this:

  * set mandatory `TENANTS` property as described in _[Mandatory common properties](#mandatory-common-properties)_ section above.
  * setup _commercetools_ and _Payone_ properties for each specific tenant name, for example, if one has `TENANTS=BOOTS, BIKES`,
    then he should explicitly set `BOOTS_CT_PROJECT_KEY`, `BIKES_CT_PROJECT_KEY`, `BOOTS_PAYONE_PORTAL_ID`, 
    `BIKES_PAYONE_PORTAL_ID` and so on.

When the service is started it initializes separate URL handlers for all specified tenants:
  * Handle payments:
    * GET <code>https://{your-service-instance.example.com}/**BOOTS**/commercetools/handle/payments/_payment-uuid_</code>
    * GET <code>https://{your-service-instance.example.com}/**BIKES**/commercetools/handle/payments/_payment-uuid_</code> 
   
  * Listen Payone Notifications:
    * POST <code>https://{your-service-instance.example.com}/**BOOTS**/payone/notification</code>
    * POST <code>https://{your-service-instance.example.com}/**BIKES**/payone/notification</code>

### Scheduled tasks

The service also provides scheduled tasks to handle those notification which were created but never handled 
(because of connection, lags, down-times etc issues). 

There are two scheduled jobs: 
  
  * short term - handles unprocessed payment transactions for last 10 minutes.
  * long term -  handles unprocessed payment transactions for last 2 days.

This jobs are run periodically based on `SHORT_TIME_FRAME_SCHEDULED_JOB_CRON` and `LONG_TIME_FRAME_SCHEDULED_JOB_CRON`
runtime values respectively. 

The job is processing every tenant payments sequentially one-by-one in order they are described in `TENANTS` property,
thus you don't need to setup the schedule for every tenant separately.