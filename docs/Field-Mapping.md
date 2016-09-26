<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [To be clarified](#to-be-clarified)
- [Payment methods covered](#payment-methods-covered)
- [PAYONE Payment methods characteristics (the missing docs)](#payone-payment-methods-characteristics-the-missing-docs)
- [API Data Mapping between PAYONE and the commercetools platform](#api-data-mapping-between-payone-and-the-commercetools-platform)
  - [commercetools Payment resource mapping](#commercetools-payment-resource-mapping)
  - [commercetools Cart and Order object (mapping to payment interface on payment creation)](#commercetools-cart-and-order-object-mapping-to-payment-interface-on-payment-creation)
  - [PAYONE fields that map to custom CT Payment fields (by payment method)](#payone-fields-that-map-to-custom-ct-payment-fields-by-payment-method)
  - [PAYONE fields that map to custom Fields on CT Cart, CT Customer or CT Order](#payone-fields-that-map-to-custom-fields-on-ct-cart-ct-customer-or-ct-order)
  - [PAYONE transaction types -> CT Transaction Types](#payone-transaction-types---ct-transaction-types)
    - [triggering a new PAYONE transaction request given a CT transaction](#triggering-a-new-payone-transaction-request-given-a-ct-transaction)
    - [receiving the PAYONE TransactionStatus Notifications and storing them in an Interaction](#receiving-the-payone-transactionstatus-notifications-and-storing-them-in-an-interaction)
    - [updating the CT Payment given a PAYONE TransactionStatus Notification (Stored in an Interaction)](#updating-the-ct-payment-given-a-payone-transactionstatus-notification-stored-in-an-interaction)
- [Unused / unsupported PAYONE fields & features](#unused--unsupported-payone-fields-&-features)
- [Constraint Rules to be implemented by the Integration](#constraint-rules-to-be-implemented-by-the-integration)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

> for better readability you might want to use a ["wide github"](https://chrome.google.com/webstore/detail/wide-github/kaalofacklcidaampbokdplbklpeldpj) plugin in your Browser

# To be clarified

With PAYONE:

 * can a cancellation happen on paypal, same for online bank transfer?
 * sofortüberweisung: can there be a chargeback / cancellation? 

# Payment methods covered

See also: [CT Method field convention](https://github.com/nkuehn/payment-integration-specifications/blob/master/Method-Keys.md)
 
| CT `method` field value |  PAYONE `clearingtype` | Additional PAYONE parameter | common name | CT payment type key |
|---|---|---|---|---|
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

# PAYONE Payment methods characteristics (the missing docs)

WORK IN PROGRESS (only active calls to PO here, no status notifications yet)
 
| common name |  PAYONE `clearingtype` | PAYONE status notification beahavior (money flow in bold). XXX müsste separat für auth /capture etc sein?  | PO calls with actual money flows | CT Authorization supported (money really reserved)? | Cancelation can happen (=Chargeback) |
|---|---|---|---|---|---|
| `DIRECT_DEBIT-SEPA` | `elv` | *capture*: appointed -> **capture** (TODO auch noch **paid**?)| auth, capture, refund, **any** debit  | NO | YES |
| `CREDIT_CARD` | `cc` | *capture*: appointed -> capture -> **paid** | auth, capture, refund, **any** debit | **YES** | YES |
| `BANK_TRANSFER_*` (all) | `sb` |  | **preauth**, auth, refund, reimbursing debit  (preauth sets no receivable, auth does) | NO | NO |
| `CASH_ADVANCE` | `vor` |  | refund, reimbursing debit | NO | NO |
| `INVOICE-DIRECT` | `rec` |  | refund, reimbursing debit | NO | NO |
| `CASH_ON_DELIVERY` | `cod` |  | refund, reimbursing debit | NO | NO |
| `WALLET-PAYPAL` | `wlt` |  | auth, capture, refund, reimbursing debit  | **YES** | YES |
| `INSTALLMENT-KLARNA` & `INVOICE-KLARNA`| `fnc` |  | refund, reimbursing debit | NO | NO |

# API Data Mapping between PAYONE and the commercetools platform
  
## commercetools Payment resource mapping

* [CT Payment documentation](http://dev.sphere.io/http-api-projects-payments.html#payment)

| CT payment JSON path | PAYONE Server API field | Who is master? |  Value transform | 
|---|---|---|---|
| id | optionally `param` | CT | No transform. For debugging purposes (find the CTP payment if txid / interfaceId are lost or accidentally changed) |
| version | (unused) | CT |  |
| customer.obj.id | `customerid` | CT | Use only as fallback to `.customerNumber` if that is not set. Extract first 20 non-dash characters to get a 20char string.  |
| customer.obj.customerNumber | `customerid` | CT | Log a Warning and ignore if the Number exceeds 20 characters. Do not truncate. |
| customer.obj.vatId | `vatId` | CT |  |
| customer.obj.dateOfBirth | `birthday` | CT | transform from ISO 8601 format (`YYYY-MM-DD`) to `YYYYMMDD`, i.e. remove the dashes |
| externalId | (unused, is intended for merchant-internal systems like ERP) | CT |  |
| interfaceId | `txid` | PAYONE |  |
| amountPlanned.centAmount | - | CT | Initially set by checkout and not modified any more. `price` from PAYONE notifications is only checked against the matching transaction amount, not the overall goal described in amountPlanned. |
| amountPlanned.currency | - | CT |  |
| amountAuthorized.centAmount | `amount` | CT / PAYONE | ONLY on CREDIT_CARD payments: Once the Authorization Transaction is in status "Success", copy the amount here.  |
| authorizedUntil | - | PAYONE | credit card payments are treated as valid seven days after the `txtime` value of the `preauthorization` call (not of other transactions!), but that is not a guarantee. Therefore it was chosen to better leave this field empty.  |
| amountPaid.centAmount | `receivable` minus `balance` | PAYONE | only if both parameters available |
| amountRefunded.centAmount | (from transactions) | PAYONE | (Sum of successful Refund Transactions) |
| paymentMethodInfo.paymentInterface | - | CT | Must be "PAYONE" in CT, otherwise do not handle the Payment at all |
| paymentMethodInfo.method | - | CT | (see the method mapping table above) |
| paymentMethodInfo.name.{locale} | - | - | (not passed, project specific content) |
| paymentStatus.interfaceCode | `status [errorcode (errormessage)]` f.i. "ERROR 917 (Refund limit exceeded)" or "APPROVED" | PAYONE | none |
| paymentStatus.interfaceText | `customermessage` if available else `status` | PAYONE | none |
| paymentStatus.state | - | - | (mapping from interfaceCode and transaction states to the Payment State Machine is project specific) |
| transactions\[\*\].id | - | CT (cannot be changed) |  |
| transactions\[\*\].timestamp | `txtime` | PAYONE (from status notification) | |
| transactions\[\*\].type |  |  | (see below for transaction types) |
| transactions\[\*\].amount.centAmount | `amount` | CT | none |
| transactions\[\*\].amount.centAmount | `capturemode` = `notcompleted` or `completed` | CT | ONLY on Charge Transactions: If the sum of Charge Transactions icluding the current one equals or exceeds the `amountPlanned` of the payment, then send `completed`, otherwise `notcompleted` Only required for Klarna payment methods.  |
| transactions\[\*\].amount.currency | `currency` | CT | none|
| transactions\[\*\].interactionId | `sequencenumber` | CT / PAYONE | *To be set when doing the PAYONE call, not already when creating the Transaction.* For transaction requests initiating a payment process (CT Authorization or CT Charge w/o CT Authorization) the `sequencenumber` is not required - PAYONE implicitly uses `0`. PAYONE posts transaction status notifications which include the `sequencenumber`. If a CT transaction (CancelAuthorization, Charge or Refund) following the initial CT transaction is added, the integration service must use the `sequencenumber` of the latest notification received from PAYONE and increment it by `1` for the new request it sends to PAYONE. Notifications are stored in the interactions array. *Please, note that PAYONE will use `sequencenumber 0` for transaction status notifications `paid` and `cancelation` related to the initiating PAYONE authorization request (CT Charge w/o CT Authorization), i.e. there will be CT transactions of different type (Charge, Chargeback) with `sequencenumber 0` in the payment.* See below for transaction status notification processing. |
| transactions\[\*\].state | - | CT / PAYONE | (see below for transaction states) |

## commercetools Cart and Order object (mapping to payment interface on payment creation)

Related Documentation:
 * [CT Order documentation](http://dev.sphere.io/http-api-projects-orders.html#order)
 * [CT Cart documentation](http://dev.sphere.io/http-api-projects-carts.html#cart)

Implementation Notes:
 * If the sum of sum of `pr[n]` times `no[n]` does not equal the total `amount` of the payment, do not pass the line item
   data at all. If the amount needs to be "fixed" to support PAYONE Invoicing or Klarna payment, this is up to the checkout
   implementation. 
 * DO NOT transfer any line item data on `refund`  or `debit` calls. 

> IMPLEMENTATION STATUS: mapping done as possible, EXCLUDING line items and custom line items (see issue #11111)

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
| lineItems\[\*\] | `it[n]` | CT (if a line Item object exists) | fixed value `goods` |
| lineItems\[\*\].id |  |  |  |
| lineItems\[\*\].name.{locale} | `de[n]` |  | truncate to 255 chars. locale to be taken from the `language` custom field of the payment  |
| lineItems\[\*\].quantity | `no[n]` | CT | fail hard if 3 chars length is exceeded |
| lineItems\[\*\].variant.id |  |  |  |
| lineItems\[\*\].variant.sku | `id[n]` | CT | truncate at 32 chars and warn |
| lineItems\[\*\].totalPrice.currencyCode |  |  |  |
| lineItems\[\*\].totalPrice.centAmount | `pr[n]` | CT | a) Divide this by .quantity to get the effective price per Line Item quantity. b) Round commercially to full cents. c) Add VAT vie .taxRate.amount if .taxRate.includedInPrice=false . d) Fail hard if 8 chars length is exceeded. |
| lineItems\[\*\].taxRate.name |  |  |  |
| lineItems\[\*\].taxRate.amount | `va[n]` | CT | .amount is a float between zero and one. It has to be multiplied with 1000 and rounded to full integer to get base % points.|
| lineItems\[\*\].taxRate.includedInPrice |  |  | (required for calculating the price, see above)  |
| customLineItems\[\*\] | `it[n]` | CT (if a custom line Item object exists) | fixed value `goods` |
| customLineItems\[\*\].id |  |  |  |
| customLineItems\[\*\].name.{locale} | `de[n]` |  | truncate to 255 chars. locale to be taken from the `language` custom field of the payment |
| customLineItems\[\*\].quantity | `no[n]` | CT | fail hard if 3 chars length is exceeded |
| customLineItems\[\*\].totalPrice.currencyCode |  |  |  |
| customLineItems\[\*\].totalPrice.centAmount | `pr[n]` | CT | a) Divide this by .quantity to get the effective price per Line Item quantity. b) Round commercially to full cents. c) Add VAT vie .taxRate.amount if .taxRate.includedInPrice=false . d) Fail hard if 8 chars length is exceeded. |
| customLineItems\[\*\].taxRate.name |  |  |  |
| customLineItems\[\*\].taxRate.amount | `va[n]` | CT | .amount is a float between zero and one. It has to be multiplied with 1000 and rounded to full integer to get base % points.|
| customLineItems\[\*\].taxRate.includedInPrice |  |  | (required for calculating the price, see above)  |
| shippingInfo.shippingMethodName | `shippingprovider`  | CT | any value starting with `DHL` is translated to `DHL` only. Any Value starting with `Bartolini` is translated to `BRT`  |
| shippingInfo.shippingMethodName | additionally `id[n]` if a shipping price is set  | CT | - |
| shippingInfo.price |  `it[n]` | CT (count n up from the last real line item if the price object exists) | fixed value `shipment` |
| shippingInfo.price |  `no[n]` | CT (count n up from the last real line item if the price object exists) | fixed value `1` |
| shippingInfo.price.currencyCode |  |  |  |
| shippingInfo.price.centAmount | `pr[n]` | CT | Add VAT vie .taxRate.amount if .taxRate.includedInPrice=false . d) Fail hard if 8 chars length is exceeded. |
| shippingInfo.taxRate.name |  |  |  |
| shippingInfo.taxRate.amount | `va[n]` | CT | .amount is a float between zero and one. It has to be multiplied with 1000 and rounded to full integer to get base % points.|
| shippingInfo.taxRate.includedInPrice |  |  | (required for calculating the price, see above)  |
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

## PAYONE fields that map to custom CT Payment fields (by payment method)

Please use the commercetools custom payment types (per method) from the [method type specifications](https://github.com/nkuehn/payment-integration-specifications/blob/master/Method-Keys.md) in the payment specifications project. 

All payment methods:

  * _Required_ `reference`: Directly taken over from a custom field `reference` of type String. Should be filled by the Order Number in the checkout / solution code.
 
`DIRECT_DEBIT`*:
  * general:
    * `bankaccountholder` -> `accountHolderName` of type String
    * `narrative_text` -> `referenceText` of type String on the Payment
    * `due_time` -> `executionTime`  of type DateTime on the Payment. CT master. convert to Unix timestamp. 
  * new data:
    * `iban` -> `IBAN` of type String (CT master)
    * `bic`  -> custom `BIC` (CT master)
  * SEPA specifics:
    * `mandate_identification` -> `sepaMandateId` of type String (CT / PAYONE master). If the checkout implementation finds an existing mandate ID for the Customer, 
      it has to set it here. Otherwise the Payment Integraion will create a new one via a `managemandate` call and store it here. The payment integration servicer
      will also automatically create a new mandate if the one given here turns out to be not valid. 
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
  * _Required_ `language` -> custom field `languageTag` of Type String on the CT Payment
    * (may be moved to the Order, Cart and Customer Objects later)
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

 1. Find Payment with PAYONE `TXID` in the Payment.interfaceId field AND paymentMethodInfo.paymentInterface = "PAYONE". You can __optionally__ check whether the `reference` fields match and whether the PO `param` field contains the CTP payment ID.   
 2. In none matches, *create one* (checkout error situation).  
   * If an *Order* with CT orderNumber = PAYONE `reference` is found, reference the payment from that
   * If a *Customer*  with CT customerNumber = PAYONE `customerid` is found, reference that customer from the payment. 
 3. Immediately apply the logic defined in the following section
 4. Add the raw TransactionStatus information as a new Interaction Object to the Payment and update (persist) the other fields (from step 3) 
 
In any case, the "TSOK" response should be sent back to PAYONE as soon as the Interaction has successfully been stored in 
commercetools (200 on the update request), but not earlier. 

> Important note: TransactionStatus Calls are coming asynchronously at no guaranteed time, but _only_ during the redirect
  flow a TransactionStatus is guaranteed to be done when the buyer is redirected back to the checkout. 
  
### updating the CT Payment given a PAYONE TransactionStatus Notification (Stored in an Interaction)

See chapter 4.2.1 "List of events (txaction)" and the sample processes in the PAYONE documentation

The matching transaction is found by sequencenumber = interactionId.
> Please note, that in case of cancelation notification of an PAYONE authorization, the sequencenumber of the CT Chargeback can collide with the sequencenumber of the initial CT Charge (w/o CT Authorization) transaction.

| PAYONE `txaction` | PAYONE `transaction_status` | PAYONE `notify_version` | CT `TransactionType` | CT `TransactionState` | Notes |
|---|---|---|---|---|---|
| `appointed` | `pending` | `7.5` |  Authorization (must be one and the first) |  Pending | create an Authorization if none there |
| `appointed` | `completed` | `7.5` | Authorization (must be one and the first | Success  | create an Authorization if none there |
| `appointed` | `completed` | `7.5` | *NOT* Authorization | Pending  | |
| `capture` | not set or `completed` | `7.5` | Charge | usually a `paid` follows separately (CT Pending), but on direct debit the `capture` already means money flow -> CT Success | create a Charge if none with matching sequencenumber there |
| `paid` | not set or `completed` | `7.5` | Charge | Success | create a Charge if no matching one is found. Does not count up the sequence number |
| `underpaid` | not set or `completed` | `7.5` | Charge  | Pending | create a Charge if no matching one found |
| `cancelation` | not set or `completed` | `7.5` | new Chargeback | Success | see 4.2.6 Sample: authorization, ELV with cancelation to derive formula for amount |
| `refund` | not set or `completed` | `7.5` | Refund | Success | create a Refund if no matching one found. |
| `debit` | not set or `completed` | `7.5` | Refund if receivable has decreased (Fee does not exist, yet) | Success if balance has decreased by the same amount, Pending otherwise | new Refund if no matching there |
| `transfer` | not set or `completed` | `7.5` | (nothing) | (nothing) TODO Warnung reinschreiben, da auf anderen Kunden umgeschaltet.   | Transfer like in "switch" / "move to" another bank account |
| `reminder` | not set or `completed` | `7.5` | (nothing) | (nothing) | status of dunning procedure |
| `vauthorization` | not set or `completed` | `7.5` | (unsupported) | (unsupported) | only available with PAYONE Billing module, must be activated |
| `vsettlement` | not set or `completed` | `7.5` | (unsupported) | (unsupported) | only available with PAYONE Billing module, must be activated |
| `invoice` | not set or `completed` | `7.5` | (nothing) | (nothing) | no status change, just write the invoice ID / URL |
| `failed` | not set or `completed` | `7.5` | (unsupported)  | (unsupported) | (not fully implemented at PAYONE yet) |

Additional fields to be updated in any case:
 * amountPaid
 * amountRefunded

# Unused / unsupported PAYONE fields & features

Unsupported PAYONE Fields
 * `creditor_*`  just for debug? 
 * `bankcountry`, `bankaccount`, `bankcode`, `bic` (all replaced by the IBAN, which is preferable because it has a checksum)
 * `xid`, `cavv`, `eci` (3Dsecure is done via redirect only) 
 * `sd[n]` "delivery date"  and `ed[n]` delivery end date (there is no matching equivalent on the CT Cart Line Item) 
 * `protect_result_avs` konfiguriert nur im backend von PAYONE. Wird quasi nicht genutzt und ist AMEX-Spezifisch. 
 *  All "traditional" bank account data fields (i.e. not IBAN and BIC) are omitted for Refund and Clearing Data Cases as 
    in these cases the Data is not put in manually by the end user. 
 * `updatereminder` when sumbitted to the PAYONE API.  We do not support manual dunning control. This has to be done per Project. 
 * Billing-Module (`vauthorization`). 

Features that have no matching semantics directly in CTP, but can be added per project (Technical Infrastructure is there, but not made public by default because that requires a stric security setup):
 *  Payment Data Check (add  GET /commercetools/payments/1234-1234-1234-1234/paymentdatacheck  call to the Service's own API )
 *  Address Check ( add GET /commercetools/order/1234-1234-1234-1234/addresscheck  to the Service's own API ) 
 *  Consumer Scoring ( add GET /commercetools/customer/1234-1234-1234-1234/consumerscore to the Service's own API)

# Constraint Rules to be implemented by the Integration

* If payment method INSTALLMENT-KLARNA or BILLSAFE are used, billing address and 
delivery address must be identical. 

