package specs.paymentmethods.creditcard;

import io.sphere.sdk.payments.Payment;
import org.apache.http.HttpResponse;
import org.concordion.api.MultiValueResult;
import specs.paymentmethods.BaseNotifiablePaymentFixture;

abstract public class BaseCreditCardChargeFixture extends BaseNotifiablePaymentFixture {

    public MultiValueResult handlePayment(final String paymentName,
                                          final String requestType) throws Exception {
        final HttpResponse response = requestToHandlePaymentByLegibleName(paymentName);
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        return MultiValueResult.multiValueResult()
                .with("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .with("interactionCount", getInteractionRequestCount(payment, transactionId, requestType))
                .with("transactionState", getTransactionState(payment, transactionId))
                .with("version", payment.getVersion().toString());
    }
}
