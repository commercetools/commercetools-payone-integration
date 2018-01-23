package specs.initialPendingMigration;

import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.SetCustomField;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.MoneyImpl;
import org.apache.http.HttpResponse;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.paymentmethods.BaseNotifiablePaymentFixture;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.commercetools.pspadapter.payone.util.PayoneConstants.PAYONE;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.substringBefore;

/**
 * Temporary test the service support both <i>Initial</i> and <i>Pending</i> transaction states to start processing.
 * After full migration to <i>Initial</i> state by CTP platform this test won't be valid any more.
 * <p>
 * Since it is a temporary test - we put everything to one class because this will be hardly supported and maintainable
 * in the future.
 *
 * @see <a href="http://dev.commercetools.com/release-notes.html#release-notes---commercetools-platform---version-release-29-september-2017">Release Notes - commercetools platform - Version Release 29 September 2017</a>
 */
@RunWith(ConcordionRunner.class)
public class InitialPendingMigrationFixture extends BaseNotifiablePaymentFixture {

    private static final String baseRedirectUrl = "https://example.com/migration_test/";

    public String createPaymentCreditCardWithout3ds(
            String paymentName,
            String paymentMethod,
            String paymentCustomType,
            String transactionType,
            String transactionState,
            String centAmount,
            String currencyCode) {

        return createPayment(paymentName, paymentMethod, paymentCustomType,
                ImmutableMap.of(CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD, getUnconfirmedVisaPseudoCardPan()),
                transactionType, transactionState, centAmount, currencyCode);
    }

    public String createPaymentCreditCard3ds(
            String paymentName,
            String paymentMethod,
            String paymentCustomType,
            String transactionType,
            String transactionState,
            String centAmount,
            String currencyCode) {

        return createPayment(paymentName, paymentMethod, paymentCustomType,
                ImmutableMap.of(CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD, getVerifiedVisaPseudoCardPan(),
                        CustomFieldKeys.SUCCESS_URL_FIELD, createRedirectUrl(baseRedirectUrl, paymentName, "Success"),
                        CustomFieldKeys.ERROR_URL_FIELD, createRedirectUrl(baseRedirectUrl, paymentName,  "Error"),
                        CustomFieldKeys.CANCEL_URL_FIELD, createRedirectUrl(baseRedirectUrl, paymentName, "Cancel")),
                transactionType, transactionState, centAmount, currencyCode);
    }

    public String createPaymentPaypal(
            String paymentName,
            String paymentMethod,
            String paymentCustomType,
            String transactionType,
            String transactionState,
            String centAmount,
            String currencyCode) {

        final String successUrl = createRedirectUrl(baseRedirectUrl, paymentName, "Success");
        final String errorUrl = createRedirectUrl(baseRedirectUrl, paymentName, "Error");
        final String cancelUrl = createRedirectUrl(baseRedirectUrl, paymentName, "Cancel");

        return createPayment(paymentName, paymentMethod, paymentCustomType,
                ImmutableMap.of(CustomFieldKeys.SUCCESS_URL_FIELD, successUrl,
                        CustomFieldKeys.ERROR_URL_FIELD, errorUrl,
                        CustomFieldKeys.CANCEL_URL_FIELD, cancelUrl),
                transactionType, transactionState, centAmount, currencyCode);
    }

    public String createSimplePayment(String paymentName, String paymentMethod, String paymentCustomType,
                                      String transactionType, String transactionState,
                                      String centAmount, String currencyCode) {
        return createPayment(paymentName, paymentMethod, paymentCustomType, emptyMap(), transactionType, transactionState, centAmount, currencyCode);
    }

    public String createKlarnaPayment(final String paymentName,
                                      final String transactionType,
                                      final String ip,
                                      final String lastName,
                                      final String birthDay,
                                      final String telephonenumber) {
        Cart cart = createTemplateCartKlarna(lastName);
        Order order = createOrderForCart(cart);

        Payment payment = createAndSaveKlarnaPayment(cart, order, paymentName, transactionType,
                MoneyImpl.centAmountOf(cart.getTotalPrice()).toString(),
                cart.getTotalPrice().getCurrency().getCurrencyCode(),
                ip, birthDay, telephonenumber);

        return payment.getId();

    }

    private String createPayment(String paymentName, String paymentMethod, String paymentCustomType,
                                 Map<String, String> specificCustomFields,
                                 String transactionType, String transactionState,
                                 String centAmount, String currencyCode) {
        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);


        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(paymentMethod)
                        .paymentInterface(PAYONE)
                        .build())
                .transactions(singletonList(TransactionDraftBuilder
                        .of(TransactionType.valueOf(transactionType), monetaryAmount)
                        .state(TransactionState.valueOf(transactionState))
                        .build()))
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        paymentCustomType,
                        ImmutableMap.<String, Object>builder().putAll(specificCustomFields)
                                .put(CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage())
                                .put(CustomFieldKeys.REFERENCE_FIELD, "<placeholder>").build()
                ))
                .build();


        final BlockingSphereClient ctpClient = ctpClient();
        final Payment payment = ctpClient.executeBlocking(PaymentCreateCommand.of(paymentDraft));
        registerPaymentWithLegibleName(paymentName, payment);

        final String orderNumber = createCartAndOrderForPayment(payment, currencyCode);

        ctpClient.executeBlocking(PaymentUpdateCommand.of(
                payment,
                ImmutableList.<UpdateActionImpl<Payment>>builder()
                        .add(SetCustomField.ofObject(CustomFieldKeys.REFERENCE_FIELD, orderNumber))
                        .build()));

        return payment.getId();
    }

    public MultiValueResult handlePayment(final String paymentName,
                                          final String requestType) throws ExecutionException, IOException {
        final HttpResponse response = requestToHandlePaymentByLegibleName(paymentName);
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        // verified only by REDIRECT payments (paypal, Secure Credit card)
        final String responseRedirectUrl = ofNullable(payment.getCustom())
                .flatMap(customFields -> ofNullable(customFields.getFieldAsString(CustomFieldKeys.REDIRECT_URL_FIELD)))
                .map(url -> substringBefore(url, "?"))
                .orElse(NULL_STRING);

        return MultiValueResult.multiValueResult()
                .with("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .with("interactionCount", getInteractionRequestCount(payment, transactionId, requestType))
                .with("transactionState", getTransactionState(payment, transactionId))
                .with("interfaceStatusCode", payment.getPaymentStatus().getInterfaceCode())
                .with("responseRedirectUrl", responseRedirectUrl)
                .with("version", payment.getVersion().toString());
    }

}
