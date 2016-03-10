package specs.paymentmethods.paypal;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.PaymentMethodInfoBuilder;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.apache.http.HttpResponse;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.BaseFixture;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author fhaertig
 * @since 21.01.16
 */
@RunWith(ConcordionRunner.class)
public class AuthorizationFixture extends BaseFixture {

    private static final String baseRedirectUrl = "https://github.com/sphereio/sphere-jvm-sdk/search?q=";

    public Map<String, String>  createPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode) throws ExecutionException, InterruptedException, UnsupportedEncodingException {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);

        final String successUrl = baseRedirectUrl + URLEncoder.encode(paymentName + " Success", "UTF-8");
        final String errorUrl = baseRedirectUrl + URLEncoder.encode(paymentName + " Error", "UTF-8");
        final String cancelUrl = baseRedirectUrl + URLEncoder.encode(paymentName + " Cancel", "UTF-8");
        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(paymentMethod)
                        .paymentInterface("PAYONE")
                        .build())
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_WALLET,
                        ImmutableMap.<String, Object>builder()
                                .put(CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage())
                                .put(CustomFieldKeys.SUCCESS_URL_FIELD, successUrl)
                                .put(CustomFieldKeys.ERROR_URL_FIELD, errorUrl)
                                .put(CustomFieldKeys.CANCEL_URL_FIELD, cancelUrl)
                                .put(CustomFieldKeys.REFERENCE_FIELD, "myGlobalKey")
                                .build()))
                .build();

        final BlockingClient ctpClient = ctpClient();
        Payment payment = ctpClient.complete(PaymentCreateCommand.of(paymentDraft));
        registerPaymentWithLegibleName(paymentName, payment);

        createCartAndOrderForPayment(payment, currencyCode);

        ctpClient.complete(PaymentUpdateCommand.of(
                payment,
                AddTransaction.of(TransactionDraftBuilder.of(
                        TransactionType.valueOf(transactionType),
                        monetaryAmount,
                        ZonedDateTime.now())
                        .state(TransactionState.PENDING)
                        .build())));

        return ImmutableMap.of(
                "paymentId", payment.getId(),
                "successUrl", successUrl,
                "cancelUrl", cancelUrl);
    }

    public Map<String, String> handlePayment(final String paymentName,
                                             final String requestType) throws ExecutionException, IOException {
        final HttpResponse response = requestToHandlePaymentByLegibleName(paymentName);
        final ZonedDateTime fetchedAt = ZonedDateTime.now(ZoneId.of("UTC"));
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        return ImmutableMap.<String, String> builder()
                .put("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .put("interactionCount", getInteractionRequestCount(payment, transactionId, requestType))
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("version", payment.getVersion().toString())
                .put("fetchedAt", fetchedAt.toString())
                .build();
    }

    public Map<String, String> fetchPaymentDetails(final String paymentName) throws InterruptedException, ExecutionException {
        Payment payment = fetchPaymentByLegibleName(paymentName);

        final String transactionId = getIdOfLastTransaction(payment);
        final String responseRedirectUrl = getInteractionRedirect(payment, transactionId)
                .map(i -> i.getFieldAsString(CustomFieldKeys.REDIRECT_URL_FIELD))
                .orElse(EMPTY_STRING);
        int urlTrimAt = responseRedirectUrl.contains("?") ? responseRedirectUrl.indexOf("?") : 0;

        return ImmutableMap.<String, String>builder()
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("responseRedirectUrlStart", responseRedirectUrl.substring(0, urlTrimAt))
                .put("responseRedirectUrlFull", responseRedirectUrl)
                .put("version", payment.getVersion().toString())
                .build();
    }

    public boolean isInteractionRedirectPresent(final String paymentName) throws ExecutionException {
        Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        return getInteractionRedirect(payment, transactionId).isPresent();
    }
}
