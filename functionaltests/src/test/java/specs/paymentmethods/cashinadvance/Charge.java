package specs.paymentmethods.cashinadvance;

import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.Payment;
import org.apache.http.HttpResponse;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.response.BasePaymentFixture;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author mht@dotsource.de
 *
 */
@RunWith(ConcordionRunner.class)
public class Charge extends BasePaymentFixture {


    public String  createPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode) throws Exception {

        final Payment payment = createAndSaveBankTransferAdvancedPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode);

        return payment.getId();
    }

    public Map<String, String> handlePayment(final String paymentName, final String requestType)
            throws ExecutionException, IOException {
        final HttpResponse response = requestToHandlePaymentByLegibleName(paymentName);
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfFirstTransaction(payment);

        return ImmutableMap.<String, String>builder()
                .put("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .put("interactionCount", getInteractionRequestCountOverAllTransactions(payment, requestType))
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("version", payment.getVersion().toString()).build();
    }

}
