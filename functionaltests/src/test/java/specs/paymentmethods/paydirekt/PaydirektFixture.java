package specs.paymentmethods.paydirekt;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.carts.CartDraftBuilder;
import io.sphere.sdk.models.Address;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.paymentmethods.BaseWalletFixture;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.neovisionaries.i18n.CountryCode.DE;
import static java.lang.String.format;

/**
 * Base class for Paydirekt payments test.
 */
@RunWith(ConcordionRunner.class)
public class PaydirektFixture extends BaseWalletFixture {
    public MultiValueResult createPayment(final String paymentName,
                                          final String paymentMethod,
                                          final String transactionType,
                                          final String centAmount,
                                          final String currencyCode) {

        return super.createPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode,
                format("https://example.com/paydirek_%s/", transactionType.toLowerCase()));
    }

    /**
     * Paydirekt has required mandatory shipping address values. See <i>TECHNICAL REFERENCE Add-On for Paydirekt (v 1.5)</i>
     * for more details
     *
     * @param currencyCode  default cart currency.
     * @param buyerLastName default buyer name in shipping/billing address.
     * @return {@link CountryCode#DE} cart draft with default shipping address values.
     */
    @Nonnull
    @Override
    protected CartDraftBuilder createDefaultCartDraftBuilder(@Nonnull String currencyCode, @Nonnull String buyerLastName) {
        return super.createDefaultCartDraftBuilder(currencyCode, "PaydirektPayerLastName")
                .shippingAddress(Address.of(DE)
                        .withFirstName("PaydirektPayerName")
                        .withLastName("PaydirektPayerLastName")
                        .withPostalCode("01001")
                        .withCity("PaydirektPayerCity"));
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
