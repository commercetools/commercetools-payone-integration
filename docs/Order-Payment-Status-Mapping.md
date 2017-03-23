# Mapping payment statuses to order payment state

When the service receives a notification from Payone about changing status of a payment we should update also 
the order's payment state.

[Payment#paymentStatus](http://dev.commercetools.com/http-api-projects-payments.html#paymentstatus) value is a info
about the latest event (notification) from Payone about payment update.

[Order#paymentState](http://dev.commercetools.com/http-api-projects-orders.html#paymentstate) is a limited list
of payment states from _commercetools platform_ (CTP).

The default mapping is implemented in `com.commercetools.pspadapter.payone.mapping.order.DefaultPaymentToOrderStateMapper`.

Here is a chart of mapping values:

<table>
<tr>
  <th>Payone txAction (payment status)</th>
  <th>CTP order payment state</th>
  <th>Comment from Payone</th>
</tr>
<tr>
  <td>appointed</td>
  <td rowspan="2"><b>Pending</b></td>
  <td></td>
</tr>
<tr>
  <td>underpaid</td>
  <td></td>
</tr>
<tr>
  <td>capture</td>
  <td rowspan="3"><b>Paid</b></td>
  <td></td>
</tr>
<tr>
  <td>paid</td>
  <td></td>
</tr>
<tr>
  <td>transfer</td>
  <td></td>
</tr>
<tr>
  <td>failed</td>
  <td rowspan="2"><b>Failed</b></td>
  <td></td>
</tr>
<tr>
  <td>cancelation</td>
  <td></td>
</tr>
<tr>
  <td>refund</td>
  <td rowspan="6">&lt;&lt;leave unchanged&gt;&gt;</td>
  <td></td>
</tr>
<tr>
  <td>debit</td>
  <td><i>debit</i> in Payone means <i>money back</i></td>
</tr>
<tr>
  <td>reminder</td>
  <td rowspan="4">Attention: This request must be activated by PAYONE.	</td>
</tr>
<tr>
  <td>vauthorization</td>
</tr>
<tr>
  <td>vsettlement</td>
</tr>
<tr>
  <td>invoice</td>
</tr>
<tr>
  <td>&lt;&lt;any other value&gt;&gt;</td>
  <td>&lt;&lt;leave unchanged&gt;&gt;</td>
  <td></td>
</tr>
</table>

**Note**: 
  - CTP value `BalanceDue` and `CreditOwed` are not mapped so far because they look unclear.
  - some payone values are unexpected or not activated, thus have no mapping and the payment status is left unchanged.

Read _PAYONE_Platform_Server_API_EN_v2.77.pdf_ for more info about payment update events.

Read http://dev.commercetools.com/ for more info about CTP Payments and Orders values
