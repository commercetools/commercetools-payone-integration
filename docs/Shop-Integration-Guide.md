# Payone Integration Service Shop Integration Guide

## Payment amount validation

  1. **Never re-use payment objects in a cart** if cart line items are changed (added/removed/quantity).
  Create a new payment object and add to the cart, don't update transactions/amount planned on existent payments.
  Omit re-usage of payment objects once payment process has been initiated (transaction added
  or customer redirected to payment provider page).

  1. On success URL (when payment has been handled by the service) **before order creation** always validate cart
  handled amount against `Payment#amountPlanned` and `Payment#transaction#amount` (because it is possible that customer
  updated the cart while payment processing was in progress).
  If the amounts don't match - report error to the shop logs and show error message to the customer
  (consider contact the shop support and initiate refund of the payment/transaction)
  and repeat payment process with a new payment.

        **Note**: the _payone-integration-service_ still doesn't have a `refund` endpoint to make automated refunding,
        but [it is int the roadmap already](https://github.com/commercetools/commercetools-payone-integration/issues/167).
        When the endpoint is implemented - the refund could be initiated automatically.
