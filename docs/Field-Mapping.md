# Data Mapping between PAYONE and the commercetools platform

> for better readability you might want to use a ["wide github"](https://chrome.google.com/webstore/detail/wide-github/kaalofacklcidaampbokdplbklpeldpj) plugin in your Browser

TODOs:
 * go through remaining open questions with CT product managers
 * go through PAYONE status notification fields and responses (currently only the requests have been documented here) 

## Payment methods covered by this specification

See also: [CT Method field convention](https://github.com/nkuehn/payment-integration-specifications/blob/master/Method-Keys.md)
 
| CT `method` field value |  PAYONE `clearingtype` | Additional PAYONE parameter | common name | CT payment type key |
|---|---|---|---|
| `DIRECT_DEBIT-SEPA` | `elv` |  | Lastschrift / Direct Debit | `payment-DIRECT_DEBIT-SEPA` |
| `CREDIT_CARD` | `cc` | (card network and specific card data are trasnferred on the client API only -> PCI DSS !) | Credit Card | `payment-CREDIT_CARD` |
| `BANK_TRANSFER-SOFORTUEBERWEISUNG` | `sb` | `onlinebanktransfertype=PNT` |  Sofortbanking / Sofortüberweisung (DE) | `payment-BANK_TRANSFER` |
| `BANK_TRANSFER-GIROPAY` | `sb` | `onlinebanktransfertype=GPY` |  Giropay (DE) | `payment-BANK_TRANSFER` |
| `BANK_TRANSFER-EPS` | `sb` | `onlinebanktransfertype=EPS` | eps / STUZZA (AT)  | `payment-BANK_TRANSFER` |
| `BANK_TRANSFER-POSTFINANCE_EFINANCE` | `sb` | `onlinebanktransfertype=PFF` | PostFinance e-Finance (CH)  | `payment-BANK_TRANSFER` |
| `BANK_TRANSFER-POSTFINANCE_CARD` | `sb` | `onlinebanktransfertype=PFC` | PostFinance Card (CH) | `payment-BANK_TRANSFER` |
| `BANK_TRANSFER-IDEAL` | `sb` | `onlinebanktransfertype=IDL` | iDEAL (NL) | `payment-BANK_TRANSFER` |
| `CASH_ADVANCE` | `vor` |  | Prepayment (PAYONE has access to the merchant's account to see if the money has arrived) | `payment-CASH_ADVANCE` |
| `INVOICE-DIRECT` | `rec` |  | Direct Invoice (PAYONE has access to the merchant's account to see if the money has arrived) | `payment-INVOICE` |
| `CASH_ON_DELIVERY` | `cod` | `shippingprovider` needs to be set, see below in the field mappings | Cash on Delivery | `payment-CASH_ON_DELIVERY` |
| `WALLET-PAYPAL` | `wlt` | `wallettype=PPE` | PayPal | `payment-WALLET`  |
| `INSTALLMENT-KLARNA` | `fnc` | `financingtype=KLS` |  Consumer Credit / Installment via Klarna | `payment-INVOICE-KLARNA` XXX TODO the fields are the same, but would it be better to differentiate? looks like a bug  |
| `INVOICE-KLARNA` | `fnc` | `financingtype=KLV` | Klarna Invoice | `payment-INVOICE-KLARNA` |

BillSAFE has been deprecated by PAYONE and is not supported. 

# API Data mapping

## TODO: ITEMS TO BE DISCUSSED

With PAYONE:

 * TODO when does `vauthorization` happen?  only on the Billing functionality?
 * TODO when does `vauthorization` happen?  only on the Billing functionality?
 * do we need to support `refund` txaction on notification if we trigger only debit and not refund? 
 * TODO when do we need the `managemandate` call?
 * TODO (probably already spoken about): which notify_versions can occur if we use the latest API version? only 7.5? 
 * hwo to calculate the amountPaid in our platform
 * clarify status. errormessage vs. failedcause vs. ...   -> what is the overall status? TODO PAYONE

With CT Product Management / CT internal:

 * TODO how to trigger address / bank data checks? Just a a payment without transaction is not safe against intermediate states and too implicit.
   (request=bankaccountcheck / addresscheck / consumerscore)
   * As data check is not a financial transaction, we don't want to include it in the Transactions. Interactions are the 
     wrong semantics. 
   * maybe directly on the integration service? ->  It has to happen synchronously in the checkout anyways.
     *  GET /commercetools/payments/1234-1234-1234-1234/paymentdatacheck
     *  GET /commercetools/payments/1234-1234-1234-1234/addresscheck
     *  GET /commercetools/payments/1234-1234-1234-1234/consumerscore

 * TODO what to write into the CT payment object to trigger an `updatereminder`, i.e. dunning level trigger
     * also directly on the integration?  POST /commercetools/payments/123.../dunninglevel  

 * The definiton of the amountAuthorized field is unclear: Is it the amount that is remaining (not yet charged) from the auth
   or is it just the sum of all successful authorizations? 

 * Wie übersetzen wir price, receivable, balance in  amountPlanned und amountPaid. Positive Balance heißt, dass der Händler noch Geld offen hat (schon bei PAYONE nachgefragt) 
   * amountPlanned = price, but the price is zero when only a preauthorization is done yet. And not in Cash Advance payments, too.  
   * amountPaid = receivable minus balance? (TODO verify with PAYONE as there is no 1:1 example yet.), Can be wrong if receivable = 0 e.g. in cash advance etc. 
   * XXX field missing XXX = receivable
     * -> How to represent due amounts that are higher (or lower) than the initial amountPlanned?  (due to dunning and chargeback fees). 
   * ACTHTUNG: PAYONE Felder in Euro, d.h. * 100 und auf volle cent runden. 

 * (Prio Low) How to set `capturemode` -> NK define logic how to find out that a capture is the last delivery.
   * e.g. IF the sum of Charge transaction amounts incl. the now to do Charge equals the current `receivable` (for which we don't have a field), THEN it's the last? 
   * (allowed values: completed / notcompleted ). Mandatory just with Billsafe & Klarna.  -> 
  
Fields that CT could consider making a built-in:

  * Reference Nr. ( a use case for the `key` feature )
  * Customer oriented status LText (`customermessage` at PAYONE )
  * something that makes the difference between originally planned amount and current receivable transparent (dunning fees etc)
  * language assignment. That's a general issue with the architecture that delegates as much as possible to a detached microservice 
    (how does the service know which of the various LText fields in CTP to take?) 

## PAYONE fields that map to custom CT Payment fields (by method)

Please use the commercetools custom payment types (per method) from the [method type specifications](https://github.com/nkuehn/payment-integration-specifications/blob/master/Method-Keys.md) in the payment specifications project. 

All payment methods:

  * _Required_ `language` -> custom field `languageTag` of Type String on the CT Payment
  * _Required_ `reference`: should conventionally be the Order Number (assuming just one payment per Order). 
     The OrderNumber is only available on the CT Order, but not the CT Cart.
     Issue at hand: Checkout Implementations vary in respect to whether the Cart is converted into an Order before or after the Order is placed. 
     Proposed behavior:
     1. check if the Order is alredy created and has an Order Number. Take that as reference.
     1. If not: Create an Order Number and store it into a custom field `reference` in the Payment object. 
        An Integration Configuration determines the Custom _Object_ container and ID from which to get the next Order ID. 
        The Checkout implementation that creates Payment before Order then needs to assure that the Order ID
        is taken from the Payment Object if the Order is created after the Payment.   
 
`DIRECT_DEBIT`*:
  * general:
    * `bankaccountholder` -> `accountHolderName` of type String
    * `narrative_text` -> `referenceText` of type String on the Payment
    * `due_time` -> `executionTime`  of type DateTime on the Payment. CT master. convert to Unix timestamp. 
  * new data:
    * `iban` -> `IBAN` of type String (CT master)
    * `bic`  -> custom `BIC` (CT master)
  * SEPA specifics:
    * `mandate_identification` -> `sepaMandateId` of type String (CT master)
    * `mandate_dateofsignature` ->  `sepaMandateDate` of type Date  (PAYONE master)
  * traditional identification:
    * `bankcountry` -> `bankCountry`  (CT master)
    * `bankcode` ->  `bankCode`  (CT master)
    * `bankbranchcode` -> `bankBranchCode` (only for FR, ES, FI, IT) (CT master) 
    * `bankaccount` -> `bankAccount`  (CT master)
    * `bankcheckdigit` -> (only for FR, BE) (CT master)
 
`BANK_TRANSFER`*:
  * general:
    * `bankaccountholder` -> `accountHolderName` of type String
    * `narrative_text` -> `referenceText` of type String on the Payment
  * new data:
    * `iban` -> `IBAN` of type String (CT initial, but overridden by PAYONE)
    * `bic`  -> custom `BIC` (CT initial, but overridden by PAYONE)
  * traditional identification:
    * `bankcountry` -> `bankCountry`
    * `bankaccount` -> `bankAccount` 
    * `bankcode` ->  `bankCode` 
    * `bankbranchcode` -> `bankBrachCode` (only for FR, ES, FI, IT)
    * `bankcheckdigit` -> `bankDataCheckDigit` (only for FR, BE)  
    * `bankgrouptype` -> custom field `bankGroupType` on Payment  (only necessary for IDEAL in NL and EPS in AT)
  * redirect flow support:
    * `redirecturl` ->  custom field `redirectUrl` of Type String on the CT Payment  (PAYONE master from response)
    * `successurl` ->  custom field `successUrl` of Type String on the CT Payment ( CT master )
    * `errorurl` -> custom field `errorUrl` Type String on Payment, CT master
    * `backurl`  -> custom field `cancelUrl` Type String on Payment, CT master
  
`CREDIT_CARD`*:
  * `narrative_text` : text on the account statements -> `referenceText` of type String on the Payment
  * card data:
    * `pseudocardpan` -> `cardDataPlaceholder` of type String (CT initial, PAYONE overrides)
    * `ecommercemode` ("internet" or "3dsecure") -> `force3DSecure` of type Boolean (CT master, PAYONE can force a redirect anyways)
    * `truncatedcardpan` (on initial call) / `cardpan` (on notifications)  -> `MaskedCardNumber` of type String (PAYONE master)
    * `cardholder`  ->  `cardHolderName`  of type String (CT master, PAYONE overrides)
    * `cardexpirydate` -> `cardExpiryDate`  of type Date. The day of the month is not of relevance, but it is recommended to put the last day of the month given. 
    * `cardtype` -> `cardNetwork` of type Enum.  key mapping self-explanatory.  
  * redirect flow support for 3Dsecure 
    * `redirecturl` ->  custom field `redirectUrl` of Type String on the CT Payment  (PAYONE master from response)
    * `successurl` ->  custom field `successUrl` of Type String on the CT Payment (CT master)
    * `errorurl` -> custom field `errorUrl` Type String on Payment, CT master
    * `backurl`  -> custom field `cancelUrl` Type String on Payment, CT master
 
`CASH_ADVANCE`:
  * refund data
   * `iban` ->  `refundIBAN` of type String
   * `bic` ->  `refundBIC`  of type String
  * Clearing data (where the invoice was paid from): 
   * `clearing_bankaccountholder` ->  `paidFromAccountHolder` 
   * `clearing_bankiban`  -> `paidFromIBAN` 
   * `clearing_bankbic` ->  `paidFromBIC` 
 
`INVOICE`*:
   * `invoiceid` invoice ID (master in PAYONE). -> `interfaceInvoiceId` custom field of type String.  PAYONE is master if invoice created by them.  For Klarna etc. CT data are master.
   * `due_time` -> custom Field `dueTime` type DateTime on Payment, CT master.  Convert to Unix timestamp.  
   * `iban` ->  `refundIBAN` of type String
   * `bic` ->  `refundBIC`  of type String
   * Clearing data (where the invoice was paid from): 
    * `clearing_bankaccountholder` ->  `paidFromAccountHolder` 
    * `clearing_bankiban`  -> `paidFromIBAN` 
    * `clearing_bankbic` ->  `paidFromBIC` 
   
`INVOICE-KLARNA`:
  * `clearing_instructionnote` ->  `invoiceUrl` field of type String, PAYONE master
  * mandatory risk management fields:
   * `personalid` -> Personal ID Nr.  Mandatory for Klarna if customers billing address is in certain nordics countries. -> `personalId` custom field of type String. 
   * `ip` -> `ipAddress` the IP address of the user is not stored in CT.  (required for Klarna)
  * redirect support:
   * `redirecturl` ->  custom field `redirectUrl` of Type String on the CT Payment  (PAYONE master from response)
   * `successurl` ->  custom field `successUrl` of Type String on the CT Payment ( CT master )
   * `errorurl` -> custom field `errorUrl` Type String on Payment, CT master
   * `backurl`  -> custom field `canceUrl` Type String on Payment, CT master
  
`WALLET`*:
  * `narrative_text` : text on the account statements -> `referenceText` of type String on the Payment     
   
## PAYONE fields that map to custom Fields on CT Cart, CT Customer or CT Order

 * _Optional_ `customermessage` and `invoiceappendix`: Check for a custom Field `description` of type `LocalizedString` on the Cart / Order. Set both PAYONE fields.
   Use the `languageTag` set on the Payment to pick the right value. 
 * _Optional_ `userid`: passed back from PAYONE as identification of the debtor account Nr.  If the CT Customer Object has a custom
    field named `payoneUserId` of type String, write the `userid` value into that field. 
  
The following are required only for Installment-Type Payment Methods (mainly Klarna): 
 
 * `birthdate` and `vatid` : these fields are only available on the CT Customer and not on the Cart. I.e. Guest checkouts
   cannot do some payment methods.
   * If the fields `dateOfBirth` (type Date) and `vatId` (type String) respectively are set as custom object on the 
     Cart / Order and have the right type they are used and take precedence over the Fields on the Customer Object
 * `gender`: Check the Cart for a custom field `customerGender`, as fallback check the Customer for a custom field 
    named `gender`. If the first existing is of Type `Enum` and has a value `Male` or `Female` -> use that one as `f` or `m` respectively.  
 * `ip`: Check for a custom Field `customerIPAddress` of type `String` on the CT Cart / Order. 
 
## unused PAYONE fields

 * `creditor_*`  just for debug? 
 * `bankcountry`, `bankaccount`, `bankcode`, `bic` (all replaced by the IBAN, which is preferable because it has a checksum)
 * `xid`, `cavv`, `eci` (3Dsecure is done via redirect only) 
 * `sd[n]` "delivery date"  and `ed[n]` delivery end date (there is no matching equivalent on the CT Cart Line Item) 
 * `protect_result_avs` konfiguriert nur im backend von PAYONE. Wird quasi nicht genutzt und ist AMEX-Spezifisch. 
 *  All "traditional" bank account data fields (i.e. not IBAN and BIC) are omitted for Refund and Clearing Data Cases as 
    in these cases the Data is not put in manually by the end user. 
 
## commercetools Payment resource

* [CT Payment documentation](http://dev.sphere.io/http-api-projects-payments.html#payment)

| CT payment JSON path | PAYONE Server API field | Who is master? |  Value transform | 
|---|---|---|---|
| id | (unused) | CT |  |
| version | (unused) | CT |  |
| customer.obj.id | `customerid` | CT | Use only as fallback to `.customerNumber` if that is not set. Extract first 20 non-dash characters to get a 20char string.  |
| customer.obj.customerNumber | `customerid` | CT | Log a Warning and ignore if the Number exceeds 20 characters. Do not truncate. |
| customer.obj.vatId | `vatId` | CT |  |
| customer.obj.dateOfBirth | `birthday` | CT | transform from ISO 8601 format (`YYYY-MM-DD`) to `YYYYMMDD`, i.e. remove the dashes |
| externalId | (unused, is intended for merchant-internal systems like ERP) | CT |  |
| interfaceId | `txid` | PAYONE |  |
| amountPlanned.centAmount | `price` | CT / PAYONE | Initially set by checkout, `price` from PAYONE notification must not deviate on Notifications. PAYONE value has to be multiplied by 100.  |
| amountPlanned.currency | - | CT |  |
| amountAuthorized.centAmount | TODO | PAYONE | TODO when the definition of the field is clarified  |
| authorizedUntil | `txtime` plus seven days | PAYONE | seven days after the txtime value of the `preauthorization` call (not of other transactions!) |
| amountPaid.centAmount | `receivable` minus `balance` | PAYONE | TODO wait for answer from PAYONE |
| amountRefunded.centAmount | (from transactions) | PAYONE | (Sum of successful Refund Transactions) |
| paymentMethodInfo.paymentInterface | - | CT | Must be "PAYONE" in CT, otherwise do not handle the Payment at all |
| paymentMethodInfo.method | - | CT | (see the method mapping table above) |
| paymentMethodInfo.name.{locale} | - | - | (not passed, project specific content) |
| paymentStatus.interfaceCode | `status` ?? TODO rather `txaction` & `transaction_status` from notifications, too? | PAYONE | none |
| paymentStatus.interfaceText | if set (on errors and invalid calls),  `errorcode` followed by a space and the  `errormessage`| PAYONE | none |
| paymentStatus.state | - | - | (mapping from interfaceCode and transaction states to the Payment State Machine is project specific) |
| transactions\[\*\].id | - | CT (cannot be changed) |  |
| transactions\[\*\].timestamp | `txtime` | PAYONE | (from status notification) |
| transactions\[\*\].type |  |  | (see below for transaction types) |
| transactions\[\*\].amount.centAmount | `amount` | CT | none |
| transactions\[\*\].amount.currency | `currency` | CT | none, but must not deviate from amountPlanned.centAmount |
| transactions\[\*\].interactionId | `sequencenumber` | CT / PAYONE | There can be only one CT Authorization transaction. This must be the first and gets the sequencenumber 0. All following Charge, CancelAuthorization and Refund transactions count up from the last sequence number received in the last TransactionStatus Notification call from PAYONE (stored in the Interactions Array). *To be set when doing the PAYONE call, not already when creating the Transaction*.  |
| transactions\[\*\].state | - | - | (see below for transaction states) |

See below for the custom fields. 

## Transaction Types and States

## PAYONE transaction types -> CT Transaction Types 

### triggering a new PAYONE transaction request given a CT transaction

Please take care of idempotency. The `TransactionState` alone does not suffice to avoid creating duplicate PAYONE transactions. 
It could remain in `Pending` for various reasons.
`interfaceId`  and `timestamp` of the Transaction can be used to manage idempotency if a persistent field is necessary. 
 
| CT `TransactionType` | CT `TransactionState` | PAYONE `request` | Notes |
|---|---|---|---|
| `Authorization` | `Pending` | `preauthorization` |  |
| `CancelAuthorization` | `Pending` | ONLY on credit card: Send a capture with amount=0.   |  |
| `Charge` | `Pending` | if an `Authorization` Transaction exists: `capture`; otherwise: `authorization`  |  |
| `Refund` | `Pending` |  `debit` with negative amount of refund. `refund` is a subset of the functionality and does not need to be used.  |  |
| `Chargeback` | - | - |  (not applicable, is just triggered from PAYONE to CT)  |


### receiving the PAYONE TransactionStatus Notifications and storing them in an Interaction

 1. Find Payment with PAYONE `TXID` in the Payment.interfaceId field AND paymentMethodInfo.paymentInterface = "PAYONE"
 2. In none matches, *create one* (checkout error situation).  
   * If an *Order* with CT orderNumber = PAYONE `reference` is found, reference the payment from that
   * If a *Customer*  with CT customerNumber = PAYONE `customerid` is found, reference that customer from the payment. 
 3. Add the raw TransactionStatus information as a new Interaction Object to the Payment (and persist)
 
The Logic defined in the following section can happen asynchroniously on the "new Interaction added" Message with the exception
of TransactionStatus Calls that happen during a redirect. The latter have to be processed immediately so the purchase
confirmation page already has the "official" result of the redirect. 
In any case, the "TSOK" response should be sent back to PAYONE as soon as the Interaction has successfully been stored in 
commercetools (200 on the update request), but not earlier. 

> Important note: TransactionStatus Calls are coming asynchronously at no guaranteed time, but _only_ during the redirect
  flow a TransactionStatus is guaranteed to be done when the buyer is redirected back to the chekcout. 
  
### updating the CT Payment given a PAYONE TransactionStatus Notification (Stored in an Interaction)

See chapter 4.2.1 "List of events (txaction)" and the sample processes in the PAYONE documentation

The matching transaction is found by sequencenumber = interactionId

[FH] PAYONE docu says that "you will receive the data and the status for each payment process". 
     So maybe this table should incooperate also the CT PaymentState?
> NK @FH: The PaymentState is an open field 

[FH] transaction_status seems to be only available with txaction "appointed" for now. 
> NK @FH: yes. And they announce that more may follow (e.g. on "paid" and "debit"). So I propose to interpret transactio_status
  not set as `completed` and expect a `pending` anytime (has effect only if our tx would otherwise move to "Success" or "Failure") 

--- EXPERIMENTAL AS DISCUSSION BASIS ---

| PAYONE `txaction` | PAYONE `transaction_status` | PAYONE `notify_version` | CT `TransactionType` | CT `TransactionState` | Notes |
|---|---|---|---|---|---|
| `appointed` | `pending` | `7.5` |  Authorization (must be one and the first) |  Pending | create an Authorization if none there |
| `appointed` | `completed` | `7.5` | Authorization (must be one and the first | Success  | create an Authorization if none there |
| `capture` | not set or `completed` | `7.5` | ??? |  |  |
| `paid` | not set or `completed` | `7.5` | last Charge? | Success | create a Charge if no matching one is found (TODO what is "matching"? one that has no sequencenumber yet?) |
| `underpaid` | not set or `completed` | `7.5` | last Charge?  | Pending | create a Charge if no matching one found |
| `cancelation` | not set or `completed` | `7.5` |  |  | TODO how to know if it's a Chargeback or a failed Charge?  |
| `refund` | not set or `completed` | `7.5` | Refund | Success | create a Refund if no matching one found. TODO do we need to support it if we don't trigger refund anyways but always use debit?  |
| `debit` | not set or `completed` | `7.5` |  |  | TODO Refund or Chargeback depending whether the `balance` is positive or negative?  |
| `transfer` | not set or `completed` | `7.5` |  |  | Transfer like in "switch"/"move to" another bank account TODO how to handle?  |
| `reminder` | not set or `completed` | `7.5` | (nothing) | (nothing) | status of dunning procedure. Just update the payment status field.  |
| `vauthorization` | not set or `completed` | `7.5` |  | (unsupported?) | only available with PAYONE Billing module, must be activated |
| `vsettlement` | not set or `completed` | `7.5` |  | (unsupported?) | only available with PAYONE Billing module, must be activated |
| `invoice` | not set or `completed` | `7.5` | (nothing) | (nothing) | no status change, just write the invoice ID / URL |
| `failed` | not set or `completed` | `7.5` | Last Charge?  | `Failure` | (not fully implemented at PAYONE yet, feedback pending) |

## commercetools Cart and Order object (mapping to payment interface on payment creation)

Related Documentation:
 * [CT Order documentation](http://dev.sphere.io/http-api-projects-orders.html#order)
 * [CT Cart documentation](http://dev.sphere.io/http-api-projects-carts.html#cart)

Implementation Notes:
 * If the sum of sum of `pr[n]` times `no[n]` does not equal the total `amount` of the payment, do not pass the line item
   data at all. If the amount needs to be "fixed" to support PAYONE Invoicing or Klarna payment, this is up to the checkout
   implementation. 

TODO 
 * map custom line items and shipping fees

| CT Cart or Order JSON path | PAYONE Server API | who is Master?  | Value transform |
|---|---|---|---|
| id |  |  |  |
| createdAt |  |  |  |
| lastModifiedAt |  |  |  |
| customerId |  |  |  |
| customerEmail |  |  |  |
| country |  |  |  |
| totalPrice.currencyCode |  |  |  |
| totalPrice.centAmount |  |  |  |
| taxedPrice.totalNet.currencyCode |  |  |  |
| taxedPrice.totalNet.centAmount |  |  |  |
| taxedPrice.totalGross.currencyCode |  |  |  |
| taxedPrice.totalGross.centAmount |  |  |  |
| taxedPrice.taxPortions\[\*\].rate |  |  |  |
| taxedPrice.taxPortions\[\*\].amount.currencyCode |  |  |  |
| taxedPrice.taxPortions\[\*\].amount.centAmount |  |  |  |
| cartState | Active/Merged/Ordered |  |  |
| lineItems\[\*\] | `it[n]` | CT (existence of a line Item) | on lineItems the value is fixed to `goods` |
| lineItems\[\*\].id |  |  |  |
| lineItems\[\*\].name.{locale} | `de[n]` |  | truncate to 255 chars. TODO how to choose the right locale & fallback locale?  |
| lineItems\[\*\].quantity | `no[n]` | CT | fail hard if 3 chars length is exceeded |
| lineItems\[\*\].variant.id |  |  |  |
| lineItems\[\*\].variant.sku | `id[n]` | CT | truncate at 32 chars and warn |
| lineItems\[\*\].totalPrice.value.currencyCode |  |  |  |
| lineItems\[\*\].totalPrice.value.centAmount | `pr[n]` | CT | a) Divide this by .quantity to get the effective price per Line Item quantity. b) Round commercially to full cents. c) Add VAT vie .taxRate.amount if .taxRate.includedInPrice=false . d) Fail hard if 8 chars length is exceeded. |
| lineItems\[\*\].taxRate.name |  |  |  |
| lineItems\[\*\].taxRate.amount | `va[n]` | CT | .amount is a float between zero and one. It has to be multiplied with 1000 and rounded to full integer to get base % points.|
| lineItems\[\*\].taxRate.includedInPrice |  |  | (required for calculating the price, see above)  |
| customLineItems\[\*\].id |  |  |  |
| customLineItems\[\*\].name.{locale} |  |  |  |
| customLineItems\[\*\].quantity |  |  |  |
| customLineItems\[\*\].money.currencyCode |  |  |  |
| customLineItems\[\*\].money.centAmount |  |  |  |
| customLineItems\[\*\].discountedPrice.value.currencyCode |  |  |  |
| customLineItems\[\*\].discountedPrice.value.centAmount |  |  |  |
| customLineItems\[\*\].taxRate.name |  |  |  |
| customLineItems\[\*\].taxRate.amount |  |  |  |
| customLineItems\[\*\].taxRate.includedInPrice |  |  |  |
| shippingAddress.title | (unused) |  |  |
| shippingAddress.salutation | (unused) |  |  |
| shippingAddress.firstName | `shipping_firstname` | CT |  |
| shippingAddress.lastName | `shipping_lastname` | CT |  |
| shippingAddress.streetName | `shipping_street` | CT |  |
| shippingAddress.streetNumber | `shipping_street` | CT | if set: append to .streetName, separated by space |
| shippingAddress.additionalStreetInfo | (unused) | CT |  |
| shippingAddress.postalCode | `shipping_zip` | CT |  |
| shippingAddress.city | `shipping_city` | CT |  |
| shippingAddress.region | (unused) |  |  |
| shippingAddress.state | `shipping_state` | CT | (only if country=US, CA, CN, JP, MX, BR, AR, ID, TH, IN), Only if an ISO-3166-2 subdivision |
| shippingAddress.country | `shipping_country` | CT |  |
| shippingAddress.company | `shipping_company` | CT |  |
| shippingAddress.department | `shipping_company` | CT | if set: append to .company, separated with a commma |
| shippingAddress.building | (unused) |  |  |
| shippingAddress.apartment | (unused) |  |  |
| shippingAddress.pOBox | (unused) |  |  |
| shippingAddress.phone | (unused) |  |  |
| shippingAddress.mobile | (unused) |  |  |
| shippingAddress.email | (unused) |  |  |
| billingAddress.title | `title` | CT |  |
| billingAddress.salutation | `salutation` | CT |  |
| billingAddress.firstName | `firstname` | CT |  |
| billingAddress.lastName | `lastname` | CT |  |
| billingAddress.company | `company` | CT |  |
| billingAddress.streetName | `street` | CT |  |
| billingAddress.streetNumber | `street` | CT | if set: append to .streetName, separated with a space |
| billingAddress.additionalStreetInfo | `addressaddition` | CT |  |
| billingAddress.postalCode | `zip` | CT |  |
| billingAddress.city | `city` | CT |  |
| billingAddress.country | `country` | CT | both use ISO 3166 |
| billingAddress.state | `state` | CT | write to PAYONE only if country=US, CA, CN, JP, MX, BR, AR, ID, TH, IN) and only if value is an ISO3166-2 subdivision |
| billingAddress.email | `email` | CT |  |
| billingAddress.phone | `telephonenumber` | CT |  |
| billingAddress.mobile | `telephonenumber` | CT | fallback value if .phone is not set |
| shippingInfo.shippingMethodName | `shippingprovider` | CT | any value starting with `DHL` is translated to `DHL` only. Any Value starting with `Bartolini` is translated to `BRT`  |
| shippingInfo.price.currencyCode |  |  |  |
| shippingInfo.price.centAmount |  |  |  |
| shippingInfo.taxRate.centAmount |  |  |  |
| shippingInfo.discountedPrice.price.currencyCode |  |  |  |
| shippingInfo.discountedPrice.price.centAmount |  |  |  |

# Constraint Rules to be implemented by the Integration

* If payment method INSTALLMENT-KLARNA or BILLSAFE are used, billing address and 
delivery address must be identical. 

