package specs.paymentmethods.cashinadvance;

import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.Payment;
import org.apache.http.HttpResponse;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.BaseFixture;
import specs.response.BasePaymentFixture;

import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author mht@dotsource.de
 *
 */
@RunWith(ConcordionRunner.class)
public class PreauthorizationFixture extends BasePaymentFixture {


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
        final String amountAuthorized = (payment.getAmountAuthorized() != null)
                ? MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized())
                : BaseFixture.EMPTY_STRING;
        final String amountPaid = (payment.getAmountPaid() != null)
                ? MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountPaid())
                : BaseFixture.EMPTY_STRING;

        return ImmutableMap.<String, String>builder()
                .put("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .put("interactionCount", getInteractionRequestCountOverAllTransactions(payment, requestType))
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("amountAuthorized", amountAuthorized)
                .put("amountPaid", amountPaid)
                .put("version", payment.getVersion().toString()).build();
    }

}
