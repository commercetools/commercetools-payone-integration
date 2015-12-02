# Data Mapping between PAYONE and the commercetools platform

TODOs:
 * go through open questions with PAYONE and CT product managers
 * go through PAYONE status notification fields and responses (currently only the requests have been documented here) 

## Payment methods covered by this specification
See also: [Method keys convention](https://github.com/nkuehn/payment-integration-specifications/blob/master/Method-Keys.md)
 
| CTP conventional key |  PAYONE `clearingtype` | Additional PAYONE parameter | common name |
|---|---|---|---|
| DIRECT_DEBIT_SEPA | `elv` |  | Lastschrift / Direct Debit |
| CREDIT_CARD | `cc` |  | Credit Card |
| BANK_TRANSFER-SOFORTUEBERWEISUNG | `sb` | `onlinebanktransfertype=PNT` |  Sofortbanking / Sofortüberweisung (DE) |
| BANK_TRANSFER-GIROPAY | `sb` | `onlinebanktransfertype=GPY` |  Giropay (DE) |
| BANK_TRANSFER-EPS | `sb` | `onlinebanktransfertype=EPS` | eps / STUZZA (AT)  |
| BANK_TRANSFER-POSTFINANCE_EFINANCE | `sb` | `onlinebanktransfertype=PFF` | PostFinance e-Finance (CH)  |
| BANK_TRANSFER-POSTFINANCE_CARD | `sb` | `onlinebanktransfertype=PFC` | PostFinance Card (CH) |
| BANK_TRANSFER-IDEAL | `sb` | `onlinebanktransfertype=IDL` | iDEAL (NL) |
| CASH_ADVANCE | TODO | TODO | TODO Prepayment is mentioned but can't find a clearingtype or other param  |
| INVOICE-DIRECT | `rec` |  | Direct Invoice |
| CASH_ON_DELIVERY | `cod` | FYI: `shippingprovider` needs to be set, see below | Cash on Delivery |
| WALLET-PAYPAL | `wlt` | `wallettype=PPE` | PayPal |
| INSTALLMENT-KLARNA | `fnc` | `financingtype=KLS` |  Consumer Credit / Installment via Klarna |
| INVOICE-KLARNA | `fnc` | `financingtype=KLV` | Klarna Invoice |

BillSAFE has been deprecated by PAYONE and is not supported. 

# API Data mapping

## TEMP: UNMAPPED / TO BE DISCUSSED PAYONE FIELDS:
 * `reference` : should be the Order Number, but the respective field on the Order is not yet 
    available during the checkout because the Cart is not yet an Order object.  Additional complexity
    is that there can be multiple payment objects per order in CT. The issue at hand is that the 
   * to stay generic, a serially increasing number should be generated and set on the payment object. 
     The checkout implementation can then choose to take this as the Order Number or not. 
   * solution 1: let CT add the `key` property to the Payment Object (which is planned anyways).
   * solution 2: let CT add the `key` or a preliminary Order ID property to the Cart and Order
   * solution 3: Use the truncated UUID of the payment object (if nothing else is configured) and store that in a 
     custom field "reference"
   * solution 4: generate a new Order Number and store that in a custom field "preliminaryOrderNumber"
 * same issue on the `invoiceid` passed to Klarna. Custom Field?  Can it be set later on? 
 * `userid` on the PAYONE side
 * `language`. Country is not enough. mapping? defaults per cart country?  It's a mandatory field for Klarna payments.
   * same problem on the line item names. how to pick the right locale? 
 * `customermessage` of an error -> was discussed at CT to add to PaymentStatus and was discarded until needed.
 * `birthdate` and `vatid` : these fields are only available on the CTP Customer and not on the Cart.
 * Generally concerning person data:  Fall back from the Cart data to the data stored in the customer account?  
   May depend on the field. For Risk management stuff like Klarna every information is crucial, but the behavior
   can also lead to errors (e.g. extra address lines).  Probably better do not do fallback. 
   ->  to the checkout this means that Klarna etc. can not be used on guest checkouts? Not nice... 
 * `gender`: there is no built-in equivalent on the CTP customer, but it is required for Klarna etc.
   * allow to configure a custom field name.  On Cart or Customer? Or both with fallback? 
 * `personalid` ->  What is the meaning of that field?  we won't store passport numbers or such. 
 * `ip` -> the IP address of the user is not stored in CTP. -> will need a custom field? (required for Klarna)
 * line item unit price `pr[n]` is that a net or a gross price?
 * the discounted line item price can have rounding errors because it is actually calculated per individual 
   quantity. i.e. the total sum calculated by PAYONE can deviate from the amount passed for the payment.
 * what's the meaning of the "delivery date" sd[n] and delivery end date ed[n] fields?  don't know how to map these.
 * TODO how to determine the sequence number -> just the index of our transactions? all transactions or
    just specific ones? (authorize = 0, charges and refunds counting 1+ , TODO chargeback?  TODO what happens if the sequence has a gap? 
   * Required only from the second capture on!  -> just count Charges in the transactions array? 
 * TODO define how to set `capturemode` -> NK discuss internally how to find out that a capture is 
   the last delivery.  ( completed / notcompleted ). Mandatory just with Billsafe & Klarna. 
 * What is the `bankbranchcode` and the ` bankcheckdigit` ? 

## TODO STUFF THAT NEEDS A STANDARD CUSTOM FIELD (in the payment-integration-specificatios repository)
 *  `narrative_text` :  this is the text on the account statements.  -> TODO custom field in the methods where it matters
 *  `redirecturl` : where to send the guy (should be multiple fields: method, URI, body)
 *  `invoiceappendix` (comment for all?) 
 *  SEPA:
  * `bankaccountholder`
  * `iban`
  * `mandate_identification`
  * `mandate_dateofsignature`
 * online transfer:
  * bankgrouptype (eps & ideal)
 * Credit CArd:
  * pseudocardpan
  * ecommercemode ("internet" or "3dsecure")
  * xid, cavv, eci  for 3dsecure???  what is this stuff? 
 * Adress verification:
  * protect_result_avs  TODO read what this is. 
   
## unused PAYONE fields
 *  `clearing_*`  prepaid, cash on delivery etc  -> necessary at all? just in an interaction? 
 *  `creditor_*`  just for debug? 
 * bankcountry, bankaccount, bankcode, bic (all replaced by the IBAN, which is preferable because it has a checksum)
 
## commercetools payment object (semantically defined base properties)

* [CT Payment documentation](http://dev.sphere.io/http-api-projects-payments.html#payment)

| CT payment JSON path | TEMPLATE Server API | Who is master of CTP value? |  Value transform | 
|---|---|---|---|
| id | (unused) | CTP |  |
| version | (unused) | CTP |  |
| customer.obj.customerNumber | `customerid` | CTP (Data on the cart precedes!) | (Requires reference expansion to be in the response data. Has to be truncated to 20.) |
| customer.obj.vatId | `vatId` | CTP |  |
| customer.obj.dateOfBirth | `birthday` | CTP | transform from ISO 8601 format (`YYYY-MM-DD`) to `YYYYMMDD`, i.e. remove the dashes |
| externalId | (unused, is intended for merchant-internal systems like ERP) | CTP |  |
| interfaceId | `txid` | PAYONE |  |
| amountPlanned.centAmount | amount | CTP | none |
| amountPlanned.currency | currency | CTP | none |
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

## commercetools payment object transaction types

| CT transaction type | TEMPLATE equivalent | Notes |
|---|---|---|
| TODO how to trigger address / bank data checks? Just a a payment without transaction is not safe | request=bankaccountcheck / addresscheck / consumerscore |  | 
| Authorization | request=preauthorization |  |
| CancelAuthorization | TODO how to do that?  |  |
| Charge | if preauthorization was done: request=capture; otherwise: request=authorization  |  |
| Refund | request=refund TODO when to use `debit`? |  |
| Chargeback | TODO this is a guess! txaction=debit? failed?  It's called "return debit note" in the documentation (on TransactionStatus call) |  |

TODO when does `vauthorization` happen? 

TODO what to write into the CTP payment object to trigger an `updatereminder`, i.e. dunning level trigger. 

TODO when do we need the `managemandate` call? 

TODO is explicit `3dscheck` (probably rather `check`?) necessary at all or implicit in the preauth/auth? 

## commercetools payment object transaction states

See chapter 4.2.1 "List of events (txaction)" in the PAYONE documentation

TODO we have to write this table per transaction type and  the other way around, i.e. 
"if this transaction status arrives, do the following". 
-> check the samples in chapter 4.2 of the documentation

| CT transaction state | TEMPLATE equivalent | Notes |
|---|---|---|
| Pending | txaction=appointed,underpaid,refund,debit,reminder,vauthorization,vsettlement,transfer,invovice (basically everything that is not Success or Fail | (initial state independently of PAYONE status) |
| Success | notify_version=(7.4|7.5) AND transaction_status=(completed,capture) ; txaction=paid |  |
| Failure | notify_version=7.5 AND txaction=failed ; txaction=cancelation  |  |

TODO verify this stuff with PAYONE

## commercetools Cart and Order object (mapping to payment interface on payment creation)

* [CT Order documentation](http://dev.sphere.io/http-api-projects-orders.html#order)
* [CT Cart documentation](http://dev.sphere.io/http-api-projects-carts.html#cart)

| CT Cart or Order JSON path | TEMPLATE Server API | TEMPLATE Client / redirect API | Value transform |
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
| lineItems\[\*\] | `it[n]` | CTP (existence of a line Item) | on lineItems the value is fixed to `goods` |
| lineItems\[\*\].id |  |  |  |
| lineItems\[\*\].name.{locale} | `de[n]` |  | truncate to 255 chars. TODO how to choose the right locale & fallback locale?  |
| lineItems\[\*\].quantity | `no[n]` | CTP | fail hard if 3 chars length is exceeded |
| lineItems\[\*\].variant.id |  |  |  |
| lineItems\[\*\].variant.sku | `id[n]` | CTP | truncate at 32 chars and warn |
| lineItems\[\*\].price.value.currencyCode |  |  |  |
| lineItems\[\*\].price.value.centAmount | `pr[n]` | CTP | overridden by .discountedPrice if that is set. Add VAT if .taxRate.includedInPrice=false . Fail hard if 8 chars length is exceeded. |
| lineItems\[\*\].price.discountedPrice.value.currencyCode |  |  |  |
| lineItems\[\*\].price.discountedPrice.value.centAmount | `pr[n]` | CTP | if set, overrides .price.value.centAmount. Add VAT if .taxRate.includedInPrice=false |
| lineItems\[\*\].taxRate.name |  |  |  |
| lineItems\[\*\].taxRate.amount | `va[n]` | CTP | .amount is a float between zero and one. It has to be multiplied with 1000 and rounded to full integer to get base % points.|
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
| shippingAddress.firstName | `shipping_firstname` | CTP |  |
| shippingAddress.lastName | `shipping_lastname` | CTP |  |
| shippingAddress.streetName | `shipping_street` | CTP |  |
| shippingAddress.streetNumber | `shipping_street` | CTP | if set: append to .streetName, separated by space |
| shippingAddress.additionalStreetInfo | (unused) | CTP |  |
| shippingAddress.postalCode | `shipping_zip` | CTP |  |
| shippingAddress.city | `shipping_city` | CTP |  |
| shippingAddress.region | (unused) |  |  |
| shippingAddress.state | `shipping_state` | CTP | (only if country=US, CA, CN, JP, MX, BR, AR, ID, TH, IN), Only if an ISO-3166-2 subdivision |
| shippingAddress.country | `shipping_country` | CTP |  |
| shippingAddress.company | `shipping_company` | CTP |  |
| shippingAddress.department | `shipping_company` | CTP | if set: append to .company, separated with a commma |
| shippingAddress.building | (unused) |  |  |
| shippingAddress.apartment | (unused) |  |  |
| shippingAddress.pOBox | (unused) |  |  |
| shippingAddress.phone | (unused) |  |  |
| shippingAddress.mobile | (unused) |  |  |
| shippingAddress.email | (unused) |  |  |
| billingAddress.title | `title` | CTP |  |
| billingAddress.salutation | `salutation` | CTP |  |
| billingAddress.firstName | `firstname` | CTP |  |
| billingAddress.lastName | `lastname` | CTP |  |
| billingAddress.company | `company` | CTP |  |
| billingAddress.streetName | `street` | CTP |  |
| billingAddress.streetNumber | `street` | CTP | if set: append to .streetName, separated with a space |
| billingAddress.additionalStreetInfo | `addressaddition` | CTP |  |
| billingAddress.postalCode | `zip` | CTP |  |
| billingAddress.city | `city` | CTP |  |
| billingAddress.country | `country` | CTP | both use ISO 3166 |
| billingAddress.state | `state` | CTP | write to PAYONE only if country=US, CA, CN, JP, MX, BR, AR, ID, TH, IN) and only if value is an ISO3166-2 subdivision |
| billingAddress.email | `email` | CTP |  |
| billingAddress.phone | `telephonenumber` | CTP |  |
| billingAddress.mobile | `telephonenumber` | CTP | fallback value if .phone is not set |
| shippingInfo.shippingMethodName | `shippingprovider` | CTP | any value starting with `DHL` is translated to `DHL` only. Any Value starting with `Bartolini` is translated to `BRT`  |
| shippingInfo.price.currencyCode |  |  |  |
| shippingInfo.price.centAmount |  |  |  |
| shippingInfo.taxRate.centAmount |  |  |  |
| shippingInfo.discountedPrice.price.currencyCode |  |  |  |
| shippingInfo.discountedPrice.price.centAmount |  |  |  |

FYI: Not all Cart / Order Fields are mentioned here because some are not relevant for the case or have no defined semantics (custom filds / product attributes). 


## Payment Method specific fields of the payment object

XXX this is just a sample. Please take the commercetools custom payment types (per method) from the [method type specifications](../../methods/) in the payment specifications project. 

### CREDIT_CARD

#### commercetools payment object custom fields

`custom.fields.` has to be prefixed when actually accessing these fields.  

| CT Payment custom property | TEMPLATE Server API | Who is Master? | Value transform |
|---|---|---|---|
| foo |  |  |  |
| foo |  |  |  |

### DIRECTDEBIT_SEPA

XXXX ... analogous to the Credit Card sample above ... 

# Constraint Rules to be implemented by the Integration

* If payment method INSTALLMENT-KLARNA or BILLSAFE are used, billing address and 
delivery address must be identical. 

