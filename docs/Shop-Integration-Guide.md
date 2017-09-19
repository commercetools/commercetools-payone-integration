# Shop Integration Guide

## Payment amount validation

  1. **Never re-use payment objects in a cart**.
  Create a new payment object and assign it to the cart if payment process has been initiated 
  (transaction added or customer redirected to payment provider page) and any of the following occurred later:
     
     - Cart has changed (items, quantity, total amount, shipping/billing address, ...)
     - Payment has changed (payment method, amount planned, currency, transaction type, ...)

  1. On success URL (when payment has been handled by the service) **before order creation** always validate cart
  handled amount against `Payment#amountPlanned` and `Payment#transaction#amount` (because it is possible that customer
  updated the cart while payment processing was in progress).
  If the amounts don't match - report error to the shop logs and show error message to the customer
  (consider contact the shop support and initiate refund of the payment/transaction)
  and repeat payment process with a new payment.

        **Note**: the _payone-integration-service_ still doesn't have a `refund` endpoint to make automated refunding,
        but [it is in the roadmap already](https://github.com/commercetools/commercetools-payone-integration/issues/167).
        When the endpoint is implemented - the refund could be initiated automatically.
