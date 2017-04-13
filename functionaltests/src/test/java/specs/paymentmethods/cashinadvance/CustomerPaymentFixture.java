package specs.paymentmethods.cashinadvance;

import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.Payment;
import org.apache.http.HttpResponse;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.BaseFixture;
import specs.paymentmethods.BaseNotifiablePaymentFixture;

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
public class CustomerPaymentFixture extends BaseNotifiablePaymentFixture {

    public String  createPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode,
            final String buyerLastName) throws Exception {

        Payment payment = createAndSaveBankTransferAdvancedPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode, buyerLastName);
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

    @Override
    public boolean receivedNotificationOfActionFor(final String paymentNames, final String txaction) throws Exception {
        // we keep this overriding just to easily see which test methods are run in this fixture
        return super.receivedNotificationOfActionFor(paymentNames, txaction);
    }

    @Override
    public boolean receivedNextNotificationOfActionFor(String paymentNames, String txaction, String prevTxaction) throws Exception {
        // we keep this overriding just to easily see which test methods are run in this fixture
        return super.receivedNextNotificationOfActionFor(paymentNames, txaction, prevTxaction);
    }

    @Override
    public String fetchOrderPaymentState(final String paymentName) {
        // we keep this overriding just to easily see which test methods are run in this fixture
        return super.fetchOrderPaymentState(getIdForLegibleName(paymentName));
    }

    public Map<String, String> fetchPaymentDetails(final String paymentName)
            throws InterruptedException, ExecutionException {
        final Payment payment = fetchPaymentByLegibleName(paymentName);

        final String transactionId = getIdOfFirstTransaction(payment);

        final long appointedNotificationCount =
                getTotalNotificationCountOfAction(payment, "appointed");
        final long paidNotificationCount = getTotalNotificationCountOfAction(payment, "paid");

        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                BaseFixture.EMPTY_STRING;

        final String amountPaid = (payment.getAmountPaid() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountPaid()) :
                BaseFixture.EMPTY_STRING;

        return ImmutableMap.<String, String>builder()
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("amountAuthorized", amountAuthorized)
                .put("amountPaid", amountPaid)
                .put("appointedNotificationCount", String.valueOf(appointedNotificationCount))
                .put("paidNotificationCount", String.valueOf(paidNotificationCount))
                .put("version", payment.getVersion().toString())
                .build();
    }
}
