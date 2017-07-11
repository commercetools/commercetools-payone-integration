<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Add Klarna payment](#add-klarna-payment)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Add Klarna payment

Update custom types: add `ip`, `gender`, `birthday`.

Klarna library updates:
  1. [Create Klarna account](https://klarna.com/buy-klarna/our-services/klarna-account), if not created yet.
     For test purpose - [create test Klarna account](https://developers.klarna.com/en/de/kpm/apply-for-test-account).
  
  2. Activate Klarna mode in Payone. **Note**: this process may take couple of days, so do it beforehand.
     
     2.1 Supply Klarna API credentials (_EID_ and _Shared Secret_) to your Payone Merchant Manager.
  
  3. Create or extend Klarna payment custom type in commercetools platform. In addition to standard _languageCode_ and _reference_
     fields add the following:

     <table>
     <tr><th>Name</th><th>Type</th><th>Required</th><th>Input Hint</th><th>Notes</th></tr>
     <tr><td><b><code>ip</code></b></td><td rowspan="4"><i>String</i></td><td rowspan="4"><b>true</b></td><td rowspan="4"><i>SingleLine</i></td><td></td></tr>
     <tr><td><b><code>gender</code></b></td><td>Only first lowercase character is used, see <a href="/blob/master/service/src/main/java/com/commercetools/pspadapter/payone/mapping/MappingUtil.java#L181-L181">MappingUtil.getGenderFromPaymentCart()</a></td></tr>
     <tr><td><b><code>birthday</code></b></td><td>If this field is empy - the service will try to apply 
                      <a href="http://dev.commercetools.com/http-api-projects-customers.html#customer">Customer's dateOfBirth</a>, 
                      but this field is an optional, also it is not available for anonymous/guest checkout. 
                      <b>Thus we stronghly recommend to set this field.</b></td></tr>
     
     </table>
     
  4. In the shop's frontend:
     
     4.1 Apply filters to the payment method, for instance, allow to select Klarna only if delivery/shipping address is
     in the list of activated countries (by default Klarna is disabled for all countries).
     
     4.2 Ensure Klarna mandatory fields from the table above are populated, also note that **`telephonenumber`** is 
     mandatory for Klarna, so ensure it is set in [`CartLike#billingAddress`](http://dev.commercetools.com/http-api-projects-carts.html#cart)
     before handling the cart/payment.
     
     4.3 Payone requires: **`billing and delivery address need to be identical`**