# Data Mapping between PAYONE and the commercetools platform

> for better readability you might want to use a ["wide github"](https://chrome.google.com/webstore/detail/wide-github/kaalofacklcidaampbokdplbklpeldpj) plugin in your Browser

TODOs:
 * go through open questions with PAYONE and CT product managers
 * go through PAYONE status notification fields and responses (currently only the requests have been documented here) 

## Payment methods covered by this specification

See also: [CT Method field convention](https://github.com/nkuehn/payment-integration-specifications/blob/master/Method-Keys.md)
 
| CT `method` field value |  PAYONE `clearingtype` | Additional PAYONE parameter | common name |
|---|---|---|---|
| `DIRECT_DEBIT_SEPA` | `elv` |  | Lastschrift / Direct Debit |
| `CREDIT_CARD` | `cc` |  | Credit Card |
| `BANK_TRANSFER-SOFORTUEBERWEISUNG` | `sb` | `onlinebanktransfertype=PNT` |  Sofortbanking / Sofortüberweisung (DE) |
| `BANK_TRANSFER-GIROPAY` | `sb` | `onlinebanktransfertype=GPY` |  Giropay (DE) |
| `BANK_TRANSFER-EPS` | `sb` | `onlinebanktransfertype=EPS` | eps / STUZZA (AT)  |
| `BANK_TRANSFER-POSTFINANCE_EFINANCE` | `sb` | `onlinebanktransfertype=PFF` | PostFinance e-Finance (CH)  |
| `BANK_TRANSFER-POSTFINANCE_CARD` | `sb` | `onlinebanktransfertype=PFC` | PostFinance Card (CH) |
| `BANK_TRANSFER-IDEAL` | `sb` | `onlinebanktransfertype=IDL` | iDEAL (NL) |
| `CASH_ADVANCE` | TODO | TODO | TODO Prepayment is mentioned but can't find a clearingtype or other param  |
| `INVOICE-DIRECT` | `rec` |  | Direct Invoice |
| `CASH_ON_DELIVERY` | `cod` | `shippingprovider` needs to be set, see below in the field mappings | Cash on Delivery |
| `WALLET-PAYPAL` | `wlt` | `wallettype=PPE` | PayPal |
| `INSTALLMENT-KLARNA` | `fnc` | `financingtype=KLS` |  Consumer Credit / Installment via Klarna |
| `INVOICE-KLARNA` | `fnc` | `financingtype=KLV` | Klarna Invoice |

BillSAFE has been deprecated by PAYONE and is not supported. 

# API Data mapping

## TODO: ITEMS TO BE DISCUSSED
 * `reference` : should be the Order Number, but the respective field on the Order is not yet 
    available during the checkout because the Cart is not yet an Order object.  Additional complexity
    is that there can be multiple payment objects per order in CT. The issue at hand is that the 
   * to stay generic, a serially increasing number should be generated and set on the payment object. 
     The checkout implementation can then choose to take this as the Order Number or not. 
   * same issue on the `invoiceid` passed to Klarna. Custom Field?  Can it be set later on? 
   * solution 1: let CT add the `key` property to the Payment Object (which is planned anyways).
   * solution 2: let CT add the `key` or a preliminary Order ID property to the Cart and Order
   * solution 3: Use the truncated UUID of the payment object (if nothing else is configured) and store that in a 
     custom field "reference"
   * solution 4: generate a new Order Number and store that in a custom field "preliminaryOrderNumber"
 
 * `userid` on the PAYONE side
 * `language`. Country is not enough. mapping? defaults per cart country?  It's a mandatory field for Klarna payments.
   * same problem on the line item names. how to pick the right locale? 
 * `customermessage` of an error -> was discussed at CT to add to PaymentStatus and was discarded until needed.
 * `birthdate` and `vatid` : these fields are only available on the CT Customer and not on the Cart.
 * `gender`: there is no built-in equivalent on the CT customer, but it is required for Klarna etc.
  * Generally concerning person data:  Fall back from the Cart data to the data stored in the customer account?  
   May depend on the field. For Risk management stuff like Klarna every information is crucial, but the behavior
   can also lead to errors (e.g. extra address lines).  Probably better do not do fallback. 
  * to the checkout this means that Klarna etc. can not be used on guest checkouts? Not nice... 
   * solution 1: allow to configure a custom field name.  On Cart or Customer? Or both with fallback? 
 * `personalid` ->  What is the meaning of that field?  we won't store passport numbers or such. 
 * `ip` -> the IP address of the user is not stored in CT. -> will need a custom field? (required for Klarna)
 * `pr[n]` (line item unit price) is that a net or a gross price?
 * the CT discounted line item price can have rounding errors because it is actually calculated per individual 
   quantity. i.e. the total sum calculated by PAYONE can deviate from the amount passed for the payment.
 *`sd[n]` "delivery date"  and `ed[n]` delivery end date fields: What is their meaning? 
 * How to determine the `sequence_number` -> just the index of our transactions? all transactions or
    just specific ones? (authorize = 0, charges and refunds counting 1+ , TODO chargeback?  TODO what happens if the sequence has a gap?
   * is the `sequence_number` 1:1 our `transaction.interfaceId`? 
   * Required only from the second capture on!  -> just count Charges in the transactions array? 
 * How to set `capturemode` -> NK discuss internally how to find out that a capture is the last delivery.
   * e.g. IF the sum of Charge transaction amounts incl. the now to do Charge equals amountPlanned, THEN it's the last? 
   * (allowed values: completed / notcompleted ). Mandatory just with Billsafe & Klarna. 
 * What is the `bankbranchcode` and the ` bankcheckdigit` ?
 * `xid`, `cavv`, `eci`  for 3dsecure???  what is this stuff?
  * `protect_result_avs`  TODO read what this is. 

## TODO NK: FIELDS THAT NEED A STANDARD CUSTOM FIELD (in the payment-integration-specificatios repository)
 *  `narrative_text` :  this is the text on the account statements.  -> TODO custom field in the methods where it matters
 *  `redirecturl` : where to send the guy (should be multiple fields: method, URI, body)
 *  `invoiceappendix` (comment for all?) 
 *  SEPA:
  * `bankaccountholder`
  * `iban`
  * `mandate_identification`
  * `mandate_dateofsignature`
 * online transfer:
  * `bankgrouptype` (eps & ideal)
 * Credit Card:
  * `pseudocardpan`
  * `ecommercemode` ("internet" or "3dsecure")
   
## unused PAYONE fields
 *  `clearing_*`  prepaid, cash on delivery etc  -> necessary at all? just in an interaction? 
 *  `creditor_*`  just for debug? 
 * `bankcountry`, `bankaccount`, `bankcode`, `bic` (all replaced by the IBAN, which is preferable because it has a checksum)
 
## commercetools Payment resource

* [CT Payment documentation](http://dev.sphere.io/http-api-projects-payments.html#payment)

| CT payment JSON path | PAYONE Server API field | Who is master? |  Value transform | 
|---|---|---|---|
| id | (unused) | CT |  |
| version | (unused) | CT |  |
| customer.obj.customerNumber | `customerid` | CT (Data on the cart precedes!) | (Requires reference expansion to be in the response data. Has to be truncated to 20.) |
| customer.obj.vatId | `vatId` | CT |  |
| customer.obj.dateOfBirth | `birthday` | CT | transform from ISO 8601 format (`YYYY-MM-DD`) to `YYYYMMDD`, i.e. remove the dashes |
| externalId | (unused, is intended for merchant-internal systems like ERP) | CT |  |
| interfaceId | `txid` | PAYONE |  |
| amountPlanned.centAmount | amount | CT | none |
| amountPlanned.currency | currency | CT | none |
| amountAuthorized |  |  |  |
| authorizedUntil |  |  |  |
| amountPaid |  |  |  |
| amountRefunded |  |  |  |
| paymentMethodInfo.paymentInterface |  |  |  |
| paymentMethodInfo.method |  |  |  |
| paymentMethodInfo.name.{locale} |  |  |  |
| paymentStatus.interfaceCode | `errorcode` OR XXX |  |  |
| paymentStatus.interfaceText | `errormessage` |  |  |
| paymentStatus.state |  |  |  |
| transactions\[\*\].id |  |  |  |
| transactions\[\*\].timestamp |  |  |  |
| transactions\[\*\].type |  |  | (see below for transaction types)  |
| transactions\[\*\].amount |  |  |  |
| transactions\[\*\].interactionId |  |  |  |
| transactions\[\*\].state |  |  | (see below for transaciton states) |

See below for the custom fields. 

## Transaction Types and States

## PAYONE transaction types -> CT Transaction Types 

### triggering a new PAYONE transaction request given a CT transaction

* TODO how to trigger address / bank data checks? Just a a payment without transaction is not safe because the integration
  service processes the Payment Object change messages asynchronously and could miss the intermediate state without a transaction. 
  * request=bankaccountcheck / addresscheck / consumerscore  
  * As data check is not a financial transaction, we don't want to include it in the Transactions. A special Interaction? But interactions
    aren't supposed to be relevant to the checkout and frontend code. 
* TODO what to write into the CT payment object to trigger an `updatereminder`, i.e. dunning level trigger  
* TODO when does `vauthorization` happen? What is it? 
* TODO when do we need the `managemandate` call? 
* TODO is explicit `3dscheck` (probably rather `check`?) necessary at all or implicit in the preauth/auth? 

Please take care of idempotency. The `TransactionState` alone does not suffice to avoid creating duplicate PAYONE transactions. 
It could remain in `Pending` for various reasons.
`interfaceId`  and `timestamp` of the Transaction can be used to manage idempotency if a persistent field is necessary. 
 
| CT `TransactionType` | CT `TransactionState` | PAYONE `request` | Notes |
|---|---|---|
| `Authorization` | `Pending` | `preauthorization` |  |
| `CancelAuthorization` | `Pending` | TODO how to do that?  |  |
| `Charge` | `Pending` | if an `Authorization` Transaction exists: `capture`; otherwise: `authorization`  |  |
| `Refund` | `Pending` | `refund` TODO when to use `debit`? |  |
| `Chargeback` | - | - |  (not applicable, is just triggered from PAYONE to CT)  |

### uppdating the CT Payment given a PAYONE TransactionStatus Notification

See chapter 4.2.1 "List of events (txaction)" and the sample processes in the PAYONE documentation

* TODO define how to find the right transaction.  `sequence_number`? 

| PAYONE `txaction` | PAYONE `transaction_status` | PAYONE `notify_version` | CT `TransactionType` | CT `TransactionState` | Notes |
|---|---|---|---|---|
| `appointed` | `` | `` | `` | `` |  |
| `capture` | `` | `` | `` | `` |  |
| `paid` | `` | `` | `` | `` |  |
| `underpaid` | `` | `` | `` | `` |  |
| `cancelation` | `` | `` | `` | `` | TODO is that  |
| `refund` | `` | `` | `` | `` |  |
| `debit` | `` | `` | `` | `` |  |
| `transfer` | `` | `` | `` | `` |  |
| `reminder` | `` | `` | `` | `` |  |
| `vauthorization` | `` | `` | `` | `` |  |
| `vsettlement` | `` | `` | `` | `` |  |
| `invoice` | `` | `` | `` | `` |  |
| `failed` | any | any | TODO all? charges? the one with the right sequence_number?  | `Failure` | (not fully implemented at PAYONE yet) |


OLD STUFF TO BE TRANSFERED:

| CT transaction state | PAYONE equivalent | Notes |
|---|---|---|
| Success | notify_version=(7.4|7.5) AND transaction_status=(completed,capture) ; txaction=paid |  |
| Failure | notify_version=7.5 AND txaction=failed ; txaction=cancelation  |  |



## commercetools Cart and Order object (mapping to payment interface on payment creation)

* [CT Order documentation](http://dev.sphere.io/http-api-projects-orders.html#order)
* [CT Cart documentation](http://dev.sphere.io/http-api-projects-carts.html#cart)

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
| lineItems\[\*\].price.value.currencyCode |  |  |  |
| lineItems\[\*\].price.value.centAmount | `pr[n]` | CT | overridden by .discountedPrice if that is set. Add VAT if .taxRate.includedInPrice=false . Fail hard if 8 chars length is exceeded. |
| lineItems\[\*\].price.discountedPrice.value.currencyCode |  |  |  |
| lineItems\[\*\].price.discountedPrice.value.centAmount | `pr[n]` | CT | if set, overrides .price.value.centAmount. Add VAT if .taxRate.includedInPrice=false |
| lineItems\[\*\].taxRate.name |  |  |  |
| lineItems\[\*\].taxRate.amount | `va[n]` | CT | .amount is a float between zero and one. It has to be multiplied with 1000 and rounded to full integer to get base % points.|
| lineItems\[\*\].taxRate.includedInPrice |  |  | (required for price field, see above)  |
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


## Payment Method specific fields of the payment object

Please take the commercetools custom payment types (per method) from the [method type specifications](https://github.com/nkuehn/payment-integration-specifications/blob/master/Method-Keys.md) in the payment specifications project. 

### CREDIT_CARD

#### commercetools payment object custom fields

`custom.fields.` has to be prefixed when actually accessing these fields.  

| CT Payment custom property | PAYONE Server API | Who is Master? | Value transform |
|---|---|---|---|
| foo |  |  |  |
| foo |  |  |  |

### DIRECTDEBIT_SEPA

XXXX ... analogous to the Credit Card sample above ... 

# Constraint Rules to be implemented by the Integration

* If payment method INSTALLMENT-KLARNA or BILLSAFE are used, billing address and 
delivery address must be identical. 

