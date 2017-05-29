package specs.paymentmethods.klarna;

import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.utils.MoneyImpl;
import org.apache.http.HttpResponse;
import org.concordion.api.FullOGNL;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.response.BasePaymentFixture;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.IP;
import static java.util.Optional.ofNullable;

@RunWith(ConcordionRunner.class)
@FullOGNL
public class KlarnaRequest extends BasePaymentFixture {

    public MultiValueResult createPayment(
            final String paymentName,
            final String transactionType,
            final String ip,
            final String lastName) throws Exception {

        Cart cart = createTemplateCartKlarna(lastName);
        cart = applyDiscounts(cart, DISCOUNT_999_CENT, DISCOUNT_10_PERCENT);
        Order order = createOrderForCart(cart);

        Payment payment = createAndSaveKlarnaPayment(cart, order, paymentName, transactionType,
                MoneyImpl.centAmountOf(cart.getTotalPrice()).toString(),
                cart.getTotalPrice().getCurrency().getCurrencyCode(),
                ip);

        Optional<CustomFields> custom = ofNullable(payment.getCustom());

        return MultiValueResult.multiValueResult()
                .with("paymentId", payment.getId())
                .with("ip", custom.map(customFields -> customFields.getFieldAsString(IP)).orElse("undefined"));
    }

    public MultiValueResult handlePayment(final String paymentName,
                                          final String requestType) throws ExecutionException, IOException {
        final HttpResponse response = requestToHandlePaymentByLegibleName(paymentName);
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);
        return MultiValueResult.multiValueResult()
                .with("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .with("interactionCount", getInteractionRequestCount(payment, transactionId, requestType))
                .with("transactionState", getTransactionState(payment, transactionId))
                .with("version", payment.getVersion().toString())
                .with("interfaceCode", payment.getPaymentStatus().getInterfaceCode());
    }

}
