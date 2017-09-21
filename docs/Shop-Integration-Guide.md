# Integration guide

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Checkout](#checkout)
- [Recalculate cart](#recalculate-cart)
- [Validate cart state](#validate-cart-state)
- [Success URL creation](#success-url-creation)
- [Validate payment amount](#validate-payment-amount)
- [Validate payment transaction](#validate-payment-transaction)
- [Check payment cancelations](#check-payment-cancelations)
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
    - [Validate cart state](#validate-cart-state)
    - [Recalculate cart](#recalculate-cart)
    - [Validate payment amount](#validate-payment-amount)
    - [Validate payment transaction](#validate-payment-transaction)
    - [Check payment cancelations](#check-payment-cancelations)

  In case all above validations were successful an order can be created right away and order confirmation page shown. Otherwise continue with payment process.

1. Add transaction of type `Authorization` or `Charge` to existing/newly created CTP payment object and call `handle URL` to trigger transaction processing against Payone payment provider.

1. Process `handle URL` response:

  **HTTP status code 200**
  - In case transaction state is `Success` create an order and show order confirmation page.

  - In case transaction state is `Pending` and `redirectUrl` is defined (which is the case of redirect payments like PayPal, Sofortüberweisung or Credit Card 3D secure) as custom field within payment object (as also within response body of the handleUrl call) then customer should be redirected to `redirectUrl`.

    - Customer successfully finalizes payment process supplied by `redirectUrl`

      - **Customer is redirected back to shop's successUrl**. After successful payment customer will get redirected to `successUrl`, which has been provided by the front end during payment object creation.  **Before order creation** some necessary validations has to be executed:

        - [Validate cart state](#validate-cart-state)
        - [Recalculate cart](#recalculate-cart)
        - [Validate payment amount](#validate-payment-amount)
        - [Check payment cancelations](#check-payment-cancelations)
        - Validate if UUID supplied within customer redirect `successUrl` matches UUID persisted within `Payment#customFields#successUrl` or `validationToken` (see [success URL creation](#success-url-creation))

        In case all above validations were successful an order can be created and order confirmation page shown.

      - **Customer successfully paid but successUrl was not reached**. In some payment redirect cases there might be a valid payment but no order as user did not reach front end's `successUrl`. For example after successfully issued payment customer loses internet connection or accidentally closes the tab. Usage of scheduled [commercetools-payment-to-order-processor](https://github.com/commercetools/commercetools-payment-to-order-processor) job ensures that for every successful payment an order can still be asynchronously created.

  **Other HTTP status codes**
  - Payment service error details are provided within response body. Payment provider specific error details in payment object (within `Payment#paymentStatus` field).

### Recalculate cart
To ensure cart totals are always up-to-date execute cart [recalculate](https://dev.commercetools.com/http-api-projects-carts.html#recalculate)

### Validate cart state
Check if current cart has been ordered already (`Cart#cartState` = `Ordered`). In this case load order by merged cart ID and show oder confirmation page. This might happen cart has been already ordered in different tab or by asynchronous process like [commercetools-payment-to-order-processor](https://github.com/commercetools/commercetools-payment-to-order-processor) job.

### Success URL creation
In order to avoid payment manipulation (re-usage of same `successUrl` for different carts) ensure that every `successUrl` includes generated unique UUID as part of the URL or as a parameter. Optionally generated UUID might be additionally persisted on payment object as custom field `validationToken` (custom type might need to be extended)

### Validate payment amount
Both `Payment#amountPlanned` and `Payment#transaction#amount` should match current cart's total amount.

If amounts do not match &mdash; show error message to the customer and consider to initiate refund (required only in case of successful transaction of type `Charge`). In this case customer will have to start a new payment process. **Note**: _payone-integration-service_ doesn't support `refund` endpoint yet but it is in the [roadmap](https://github.com/commercetools/commercetools-payone-integration/issues/167) already.

### Validate payment transaction
Payment counts as successful if there is at least one successful (`Payment#Transaction#state`=`Success`)
payment transaction of type `Charge` or `Authorization`.

### Check payment cancelations
Successful payment might still be invalidated (**edge case**) if there is at least one successful (`#Transaction#state`=`Success`) transaction of **other** type than `Charge` or `Authorization` and `#Transaction#amount` greater than `0`.

### Bad practice
- Never delete or un-assign created payment objects **during checkout** from the cart. If required &mdash; clean up  unused/obsolete payment objects by another asynchronous process instead.
