# Integration guide

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Checkout](#checkout)
- [Recalculate cart](#recalculate-cart)
- [Validate cart state](#validate-cart-state)
- [Validate payment amount](#validate-payment-amount)
- [Validate payment transaction](#validate-payment-transaction)
- [Check payment cancelations](#check-payment-cancelations)
- [Refund automation](#automation-of-refunds-for-wrong-transactions)
- [Bad practice](#bad-practice)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

### Checkout

1. On each checkout step [validate cart state](#validate-cart-state)

1. Show available payment methods. If cart has already payment objects then front end could optionally use most recently created (get last element or use `Payment#createdAt`) payment object in order to preselect customer's last payment method selection.

1. Once customer selected payment create CTP payment object and set required fields as described [here](https://github.com/commercetools/commercetools-payone-integration/blob/master/docs/Field-Mapping.md). **Note**: ensure new payment object is created (**do not reuse existing one**) if:
    - Cart has changed (items, quantity, total amount, shipping/billing address, ...)
    - Customer selected payment method (does not matter if same as before or not)
    - Payment has changed (amount planned, currency, transaction type, ...)

1. Assign created payment object to the cart (see also [bad practice](#bad-practice))

1. Before customer click's on `Pay and order` ensure to execute following steps:
    - [Payment validation](#payment-validation)

  In case above validations were successful an order can be created right away and order confirmation page shown. Otherwise continue with payment process.

1. Add transaction of type `Authorization` or `Charge` to existing/newly created CTP payment object and call `handle URL` to trigger transaction processing against Payone payment provider.

1. Process `handle URL` response:

  **HTTP status code 200**
  - In case transaction state is `Success` create an order and show order confirmation page.

  - In case transaction state is `Pending` and `redirectUrl` is defined (which is the case of redirect payments like PayPal, Sofortüberweisung or Credit Card 3D secure) as custom field within payment object (as also within response body of the handleUrl call) then customer should be redirected to `redirectUrl`.

    - Customer successfully finalizes payment process supplied by `redirectUrl`

      - **Customer is redirected back to shop's successUrl**. After successful payment customer will get redirected to `successUrl`, which has been provided by the front end during payment object creation.  **Before order creation** some necessary validations has to be executed:
        - [Payment validation](#payment-validation)

        In case above validations were successful an order can be created and order confirmation page shown.

      - **Customer successfully paid but successUrl was not reached**. In some payment redirect cases there might be a valid payment but no order as user did not reach front end's `successUrl`. For example after successfully issued payment customer loses internet connection or accidentally closes the tab. Usage of scheduled [commercetools-payment-to-order-processor](https://github.com/commercetools/commercetools-payment-to-order-processor) job ensures that for every successful payment an order can still be asynchronously created.

  **Other HTTP status codes**
  - Payment service error details are provided within response body. Payment provider specific error details in payment object (within `Payment#paymentStatus` field).

### Payment validation
  - [Validate cart state](#validate-cart-state)
  - [Recalculate cart](#recalculate-cart)
  - [Validate payment amount](#validate-payment-amount)
  - [Validate payment transaction](#validate-payment-transaction).
  - [Check payment cancelations](#check-payment-cancelations)

### Validate cart state
Check if current cart has been ordered already (`Cart#cartState` = `Ordered`). In this case load order by merged cart ID and show oder confirmation page. This might happen cart has been already ordered in different tab or by asynchronous process like [commercetools-payment-to-order-processor](https://github.com/commercetools/commercetools-payment-to-order-processor) job.

### Recalculate cart
To ensure cart totals are always up-to-date execute cart [recalculate](https://dev.commercetools.com/http-api-projects-carts.html#recalculate)

### Validate payment amount
Both `Payment#amountPlanned` and `Payment#transaction#amount` should match current cart's total amount. 
**Scope**: all payments objects of the cart.

If amounts do not match &mdash; show error message to the customer and consider to initiate refund (required only in case of successful transaction of type `Charge`). In this case customer will have to start a new payment process. **Note**: _payone-integration-service_ doesn't support `refund` endpoint yet but it is in the [roadmap](https://github.com/commercetools/commercetools-payone-integration/issues/167) already. See also [refund automation](#automation-of-refunds-for-wrong-transactions).

### Validate payment transaction
Payment counts as successful if there is at least one successful (`Payment#Transaction#state`=`Success`)
payment transaction of type `Charge` or `Authorization`. 
**Scope**: all payments objects of the cart.

### Check payment cancelations
Successful payment might still be invalidated (**edge case**) if there is at least one successful (`#Transaction#state`=`Success`) transaction of **other** type than `Charge` or `Authorization` and `#Transaction#amount` greater than `0`.
**Scope**: payment object with succesful transaction of type `Charge` or `Authorization`.

### Automation of refunds
For redirect payments payment amount is bound to customer's `redirectUrl`. After redirect and before actual finalisation of the payment at provider's page, customer is still able to change the cart's amount within the second tab. If customer decides to change cart's amount within the second tab and finalise payment within the first tab then according to [payment amount validation](#validate-payment-amount) an error will be shown and order creation declined. If customer runs into this scenario and used transaction type was `Charge` than there should be setup an automated asychronous process, which would trigger refund for succesful(`#Transaction#state`=`Success`) `Charge` transactions where transaction amount does not match cart's amount.
Additionally it would ensure that each cart has only one valid (matches cart's amount, is of type `Charge` and is succesful `#Transaction#state`=`Success`) transaction and would refund all the others (left transaction should be the one where `#Payment#custom#fields#referenceId` is the same as `#Order#orderNumber`). Process might be based on following [events](https://dev.commercetools.com/http-api-projects-messages.html#paymenttransactionstatechanged-message).

### Bad practice
- Never delete or un-assign created payment objects **during checkout** from the cart. If required &mdash; clean up  unused/obsolete payment objects by another asynchronous process instead.
