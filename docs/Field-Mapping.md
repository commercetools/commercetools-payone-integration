# Data Mapping between PAYONE and the commercetools platform


## Payment methods covered by this specification
See also: [Method keys convention](https://github.com/nkuehn/payment-integration-specifications/blob/master/Method-Keys.md)
 
| CTP conventional key |  PAYONE `clearingtype` | Additional PAYONE parameter | common name |
|---|---|---| 
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
| CASH_ON_DELIVERY | `cod` | FYI: `shippingprovider` needs to be set from the Cart / Order data | Cash on Delivery |
| WALLET-PAYPAL | `wlt` | `wallettype=PPE` | PayPal |
| INSTALLMENT-KLARNA | `fnc` | `financingtype=KLS` |  Consumer Credit / Installment via Klarna |
| INVOICE-KLARNA | `fnc` | `financingtype=KLV` | Klarna Invoice |

BillSAFE has been deprecated by PAYONE and is not supported. 

# API Data mapping

## commercetools payment object (semantically defined base properties)

* [CT Payment documentation](http://dev.sphere.io/http-api-projects-payments.html#payment)

| CT payment JSON path | TEMPLATE Server API | Who is master? |  Value transform | 
|---|---|---|---|
| id |  |  |  |
| version |  |  |  |
| customer |  |  |  |
| externalId |  |  |  |
| interfaceId |  |  |  |
| amountPlanned |  |  |  |
| amountAuthorized |  |  |  |
| authorizedUntil |  |  |  |
| amountPaid |  |  |  |
| amountRefunded |  |  |  |
| paymentMethodInfo.paymentInterface |  |  |  |
| paymentMethodInfo.method |  |  |  |
| paymentMethodInfo.name.{locale} |  |  |  |
| paymentStatus.interfaceCode |  |  |  |
| paymentStatus.interfaceText |  |  |  |
| paymentStatus.state |  |  |  |
| transactions\[\*\].id |  |  |  |
| transactions\[\*\].timestamp |  |  |  |
| transactions\[\*\].type |  | TODO type mapping table |  |
| transactions\[\*\].amount |  |  |  |
| transactions\[\*\].interactionId |  |  |  |
| transactions\[\*\].state |  |  | TODO state mapping |

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
| lineItems\[\*\].id |  |  |  |
| lineItems\[\*\].name.{locale} |  |  |  |
| lineItems\[\*\].quantity |  |  |  |
| lineItems\[\*\].variant.id |  |  |  |
| lineItems\[\*\].variant.sku |  |  |  |
| lineItems\[\*\].price.value.currencyCode |  |  |  |
| lineItems\[\*\].price.value.centAmount |  |  |  |
| lineItems\[\*\].price.discountedPrice.value.currencyCode |  |  |  |
| lineItems\[\*\].price.discountedPrice.value.centAmount |  |  |  |
| lineItems\[\*\].taxRate.name |  |  |  |
| lineItems\[\*\].taxRate.amount |  |  |  |
| lineItems\[\*\].taxRate.includedInPrice |  |  |  |
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
| shippingAddress.title |  |  |  |
| shippingAddress.salutation |  |  |  |
| shippingAddress.firstName |  |  |  |
| shippingAddress.lastName |  |  |  |
| shippingAddress.streetName |  |  |  |
| shippingAddress.streetNumber |  |  |  |
| shippingAddress.additionalStreetInfo |  |  |  |
| shippingAddress.postalCode |  |  |  |
| shippingAddress.city |  |  |  |
| shippingAddress.region |  |  |  |
| shippingAddress.state |  |  |  |
| shippingAddress.country |  |  |  |
| shippingAddress.company |  |  |  |
| shippingAddress.department |  |  |  |
| shippingAddress.building |  |  |  |
| shippingAddress.apartment |  |  |  |
| shippingAddress.pOBox |  |  |  |
| shippingAddress.phone |  |  |  |
| shippingAddress.mobile |  |  |  |
| shippingAddress.email |  |  |  |
| billingAddress.* |  | (analogous to shippingAddress) |  |
| shippingInfo.shippingMethodName |  |  |  |
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

