# Integration guide

### Payment process:
- Show available payment methods

- Once customer selected payment; create CTP payment object and set required fields as described [here](https://github.com/commercetools/commercetools-payone-integration/blob/master/docs/Field-Mapping.md). **Note**: ensure new payment object is created (**do not reuse existing one**) if:
  - Cart has changed (items, quantity, total amount, shipping/billing address, ...)
  - Customer selected payment method (does not matter if same as before or not)
  - Payment has changed (amount planned, currency, transaction type, ...)

- Assign created payment object to the cart

- Once customer is ready to pay and order make sure:
  - `Cart#cartState` is `Active`. In case it is `Merged` order (order can be fetched by merged cart ID) confirmation page should be shown.

  - Ensure cart totals are up to date (execute cart [recalculate](https://dev.commercetools.com/http-api-projects-carts.html#recalculate))

  - [Validate payment](#validate-payment). In case of successful validation order can be created right away and order confirmation page shown.

- Add transaction of type `Authorization` or `Charge` to existing/newly created CTP payment object and call `handle URL` to trigger transaction processing against Payone payment provider.
- Process `handle URL` response:

  **HTTP status code 200**
  - In case transaction state is `Success` create an order and show order confirmation page.

  - In case transaction state is `Pending` and `redirectUrl` is defined (which is the case of redirect payments like PayPal, Sofortüberweisung or Credit Card 3D secure) as custom field within payment object (as also within response body of the handleUrl call) then customer should be redirected to `redirectUrl`.

    - Customer successfully finalizes payment process supplied by `redirectUrl`

      - **Customer is redirected back to shop's successUrl**. After successful payment customer will get redirected to `successUrl`, which has been provided by the front end during payment object creation.  **Before order creation** some necessary validations has to be executed:

        - Check if current cart has been ordered already (`Cart#cartState` = `Merged`). This might happen if multiple tabs are in use. In this case load order by merged cart ID and show oder confirmation page.

        - Ensure cart totals are up to date (execute cart [recalculate](https://dev.commercetools.com/http-api-projects-carts.html#recalculate))

        - Validate if UUID supplied within customer redirect `successUrl` matches UUID persisted within `Payment#customFields#successUrl` or `validationToken` (see [Success URL creation](#success-url-creation))

        - [Validate payment](#validate-payment). If amounts do not match &mdash; show error message to the customer and consider to initiate refund (required only in case of successful transaction of type `Charge`). In this case customer will have to start a new payment process. **Note**: _payone-integration-service_ doesn't support `refund` endpoint yet but it is in the [roadmap](https://github.com/commercetools/commercetools-payone-integration/issues/167) already.

        - In case all above validations were successful an order can be created and order confirmation page shown.

      - **Customer successfully paid but successUrl was not reached**. In some payment redirect cases there might be a valid payment but no order as user did not reach front end's `successUrl`. For example after successfully issued payment customer loses internet connection or accidentally closes the tab. Usage of scheduled [commercetools-payment-to-order-processor](https://github.com/commercetools/commercetools-payment-to-order-processor) job ensures that for every successful payment an order can still be asynchronously created.

  **Other HTTP status codes**
  - Payment service error details are provided within response body. Payment provider specific error details in payment object (within `Payment#paymentStatus` field).

### Success URL creation
In order to avoid payment manipulation (re-usage of same `successUrl` for different carts) ensure that every `successUrl` includes generated unique UUID as part of the URL or as a parameter. Optionally generated UUID might be additionally persisted on payment object as custom field `validationToken` (custom type might need to be extended)

### Validate payment
Payment counts as successful if there is at least one successful (`Payment#Transaction#state`=`Success`) payment transaction of type `Charge` or `Authorization` where both `Payment#amountPlanned` and `Payment#transaction#amount` matches current cart's total amount. Successful payment might still be invalidated (**edge case**) if there is at least one successful (`#Transaction#state`=`Success`) transaction of **other** type than `Charge` or `Authorization` and `#Transaction#amount` greater than `0`.

### Bad practice
- Never delete or un-assign created payment objects **during checkout** from the cart. If required &mdash; clean up  unused/obsolete payment objects by another asynchronous process instead.