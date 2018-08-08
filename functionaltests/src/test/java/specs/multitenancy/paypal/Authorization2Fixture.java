package specs.multitenancy.paypal;

import io.sphere.sdk.payments.Payment;
import org.apache.http.HttpResponse;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.multitenancy.BaseTenant2Fixture;

@RunWith(ConcordionRunner.class)
public class Authorization2Fixture extends BaseTenant2Fixture {

    public MultiValueResult createPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode) {

        Payment payment = createAndSaveWalletPayment(paymentName, paymentMethod, transactionType,
                centAmount, currencyCode,
                "https://example.com/migration/paypal_authorization/");

        return MultiValueResult.multiValueResult()
                .with("paymentId", payment.getId());
    }

    public MultiValueResult handlePayment(final String paymentName,
                                          final String requestType) throws Exception {
        final HttpResponse response = requestToHandlePaymentByLegibleName(paymentName);

        // explicitly fetch from second tenant CTP client instance
        final Payment payment = fetchPaymentByNameFromTenant2(paymentName);

        final String transactionId = getIdOfLastTransaction(payment);

        return MultiValueResult.multiValueResult()
                .with("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .with("interactionCount", getInteractionRequestCount(payment, transactionId, requestType))
                .with("transactionState", getTransactionState(payment, transactionId));
    }
}
