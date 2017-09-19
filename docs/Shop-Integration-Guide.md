# Shop Integration Guide

## Payment amount validation

  1. **Never re-use payment objects in a cart**.
  Create a new payment object and assign it to the cart if payment process has been initiated 
  (transaction added or customer redirected to payment provider page) and any of the following occurred later:
     
     - Cart has changed (items, quantity, total amount, shipping/billing address, ...)
     - Payment has changed (payment method, amount planned, currency, transaction type, ...)
  
  1. Before letting customer to initiate or retry payment process always check
  if there are any payments with transactions of type `Authorization` or `Charge` and state `Success`.
  In those cases create an order right away and show order confirmation page.
  To avoid re-usage of same success URL for different checkouts/carts (payment manipulation) &mdash;
  make sure that cart has successful payment (transactions of type `Authorization` or `Charge` and state `Success`).

  1. On success URL (when payment has been handled by the service) **before order creation** ensure:

     - payment's `Payment#amountPlanned` and `Payment#transaction#amount` match total amount of the current cart 
     (it is possible that customer updated the cart while payment processing was in progress, for example &mdash; redirect payments). 
     
       If the amounts do not match &mdash; show error message to the customer and consider to initiate refund 
       of the successfully executed payment transaction. In this case customer will have to start a new payment process. 
  
       **Note**: the _payone-integration-service_ still doesn't support `refund` endpoint to make automated refunding,
       but [it is in the roadmap already](https://github.com/commercetools/commercetools-payone-integration/issues/167).
       When the endpoint is implemented - the refund process could be initiated by shop service (front/back end).
