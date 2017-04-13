package specs.paymentmethods.creditcard;

import io.sphere.sdk.payments.Payment;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Jan Wolter
 */
@RunWith(ConcordionRunner.class)
public class ChargeImmediatelyWithout3dsFixture extends BaseCreditCardChargeFixture {

    public String createPayment(final String paymentName,
                                final String paymentMethod,
                                final String transactionType,
                                final String centAmount,
                                final String currencyCode) throws Exception {

        Payment payment = createAndSaveCardPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode);
        return payment.getId();
    }

    @Override
    public MultiValueResult handlePayment(final String paymentName,
                                          final String requestType) throws Exception {
        return super.handlePayment(paymentName, requestType);
    }

    public Map<String, String> fetchPaymentDetails(final String paymentName) throws InterruptedException, ExecutionException {
        return fetchCreditCardPaymentDetails(paymentName);
    }

    @Override
    public boolean receivedNotificationOfActionFor(final String paymentNames, final String txaction) throws Exception {
        // we keep this overriding just to easily see which test methods are run in this fixture
        return super.receivedNotificationOfActionFor(paymentNames, txaction);
    }

    @Override
    public String fetchOrderPaymentState(final String paymentName) {
        // we keep this overriding just to easily see which test methods are run in this fixture
        return super.fetchOrderPaymentState(getIdForLegibleName(paymentName));
    }

    public long getInteractionNotificationCountOfAction(final String paymentName, final String txaction) throws ExecutionException {
        Payment payment = fetchPaymentByLegibleName(paymentName);
        return getTotalNotificationCountOfAction(payment, txaction);
    }
}
