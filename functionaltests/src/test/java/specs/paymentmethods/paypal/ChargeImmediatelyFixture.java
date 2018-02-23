package specs.paymentmethods.paypal;

import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author Jan Wolter
 */
@RunWith(ConcordionRunner.class)
public class ChargeImmediatelyFixture extends BaseWalletFixture {

    public MultiValueResult createPayment(final String paymentName,
                                          final String paymentMethod,
                                          final String transactionType,
                                          final String centAmount,
                                          final String currencyCode) {

        return super.createPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode,
                "https://example.com/paypal_charge/");
    }

    @Override
    public MultiValueResult handlePayment(final String paymentName,
                                          final String requestType) throws ExecutionException, IOException {
        return super.handlePayment(paymentName, requestType);
    }

    @Override
    public MultiValueResult fetchPaymentDetails(final String paymentName) throws ExecutionException {
        return super.fetchPaymentDetails(paymentName);
    }

    @Override
    public boolean isInteractionRedirectPresent(final String paymentName) throws ExecutionException {
        return super.isInteractionRedirectPresent(paymentName);
    }
}
