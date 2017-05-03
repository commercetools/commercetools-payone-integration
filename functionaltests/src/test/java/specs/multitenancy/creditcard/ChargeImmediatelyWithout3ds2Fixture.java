package specs.multitenancy.creditcard;

import io.sphere.sdk.payments.Payment;
import model.HandlePaymentResult;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.BaseFixture;
import specs.multitenancy.BaseTenant2Fixture;

import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Test CreditCard payment handling and notifications on second tenant.
 */
@RunWith(ConcordionRunner.class)
public class ChargeImmediatelyWithout3ds2Fixture extends BaseTenant2Fixture {

    public String createPayment(final String paymentName,
                                final String paymentMethod,
                                final String transactionType,
                                final String centAmount,
                                final String currencyCode) throws Exception {
        final Payment payment = createAndSaveCardPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode);
        return payment.getId();
    }

    public MultiValueResult handlePayment(final String paymentName) throws ExecutionException, IOException {
        HandlePaymentResult handlePaymentResult = handlePaymentByName(paymentName);
        final String transactionId = getIdOfLastTransaction(handlePaymentResult.getPayment());

        final String amountPlanned = (handlePaymentResult.getPayment().getAmountPlanned() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(handlePaymentResult.getPayment().getAmountPlanned()) :
                BaseFixture.EMPTY_STRING;

        return MultiValueResult.multiValueResult()
                .with("statusCode", Integer.toString(handlePaymentResult.getHttpResponse().getStatusLine().getStatusCode()))
                .with("transactionState", getTransactionState(handlePaymentResult.getPayment(), transactionId))
                .with("amountPlanned", amountPlanned);
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
    public boolean receivedNextNotificationOfActionFor(String paymentNames, String txaction, String prevTxaction) throws Exception {
        // we keep this overriding just to easily see which test methods are run in this fixture
        return super.receivedNextNotificationOfActionFor(paymentNames, txaction, prevTxaction);
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
