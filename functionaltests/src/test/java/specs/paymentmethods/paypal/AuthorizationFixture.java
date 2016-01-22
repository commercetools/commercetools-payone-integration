package specs.paymentmethods.paypal;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.PaymentMethodInfoBuilder;
import io.sphere.sdk.payments.TransactionDraft;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.apache.http.HttpResponse;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import specs.BaseFixture;
import util.WebDriverPaypal;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author fhaertig
 * @since 21.01.16
 */
@RunWith(ConcordionRunner.class)
public class AuthorizationFixture extends BaseFixture {

    private WebDriverPaypal webDriver;

    private static final String baseRedirectUrl = "http://dev.commercetools.com/search.html?stp=1&stq=";

    private static final Splitter thePaymentNamesSplitter = Splitter.on(", ");

    private Map<String, String> successUrlForPayment;

    @Before
    public void setUp() {
        webDriver = new WebDriverPaypal();
    }

    @After
    public void tearDown() {
        webDriver.quit();
    }

    public String createPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode) throws ExecutionException, InterruptedException, UnsupportedEncodingException {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);

        final List<TransactionDraft> transactions = Collections.singletonList(TransactionDraftBuilder
                .of(TransactionType.valueOf(transactionType), monetaryAmount, ZonedDateTime.now())
                .state(TransactionState.PENDING)
                .build());

        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(paymentMethod)
                        .paymentInterface("PAYONE")
                        .build())
                .transactions(transactions)
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_WALLET,
                        ImmutableMap.<String, Object>builder()
                                .put(CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage())
                                .put(CustomFieldKeys.SUCCESS_URL_FIELD, baseRedirectUrl + URLEncoder.encode(paymentName + " Success", "UTF-8"))
                                .put(CustomFieldKeys.ERROR_URL_FIELD, baseRedirectUrl + URLEncoder.encode(paymentName + " Error", "UTF-8"))
                                .put(CustomFieldKeys.CANCEL_URL_FIELD, baseRedirectUrl + URLEncoder.encode(paymentName + " Cancel", "UTF-8"))
                                .put(CustomFieldKeys.REFERENCE_FIELD, "myGlobalKey")
                                .build()))
                .build();

        final BlockingClient ctpClient = ctpClient();
        Payment payment = ctpClient.complete(PaymentCreateCommand.of(paymentDraft));
        registerPaymentWithLegibleName(paymentName, payment);

        createCartAndOrderForPayment(payment, currencyCode);

        return payment.getId();
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
                .put("responseRedirectUrl", responseRedirectUrl.substring(0, urlTrimAt))
                .put("version", payment.getVersion().toString())
                .build();
    }

    public  Map<String, String> executeRedirectAndWaitForNotificationOfAction(final String paymentName, final String txaction) throws ExecutionException, InterruptedException {

        Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);
        final String responseRedirectUrl = payment.getCustom().getFieldAsString(CustomFieldKeys.REDIRECT_URL_FIELD);

        String successUrl = new WebDriverPaypal().executePaypalPayment(responseRedirectUrl, getTestDataPaypalEmail(), getTestDataPaypalPassword());
        successUrl = successUrl.replace(baseRedirectUrl, "[...]");

        //wait just a little until notification was processed (is triggered immediately after verification)
        Thread.sleep(100);

        payment = fetchPaymentById(payment.getId());
        long appointedNotificationCount = getInteractionNotificationCountOfAction(payment, txaction);

        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                BaseFixture.EMPTY_STRING;

        return ImmutableMap.<String, String> builder()
                .put("appointedNotificationCount", String.valueOf(appointedNotificationCount))
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("amountAuthorized", amountAuthorized)
                .put("successUrl", successUrl)
                .put("version", payment.getVersion().toString())
                .build();
    }

    public boolean isInteractionRedirectPresent(final String paymentName) throws ExecutionException {
        Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        return getInteractionRedirect(payment, transactionId).isPresent();
    }


 /*protected  void resetCommercetoolsPlatform() {
        // TODO jw: use futures
        // delete all orders
        ctpClient().complete(OrderQuery.of().withLimit(500)).getResults()
                .forEach(order -> ctpClient().complete(OrderDeleteCommand.of(order)));

        // delete all carts
        ctpClient().complete(CartQuery.of().withLimit(500)).getResults()
                .forEach(cart -> ctpClient().complete(CartDeleteCommand.of(cart)));

        // delete all payments
        ctpClient().complete(PaymentQuery.of().withLimit(500)).getResults()
                .forEach(payment -> ctpClient().complete(PaymentDeleteCommand.of(payment)));

         ctpClient().complete(TypeQuery.of().withLimit(500)).getResults()
                 .forEach(type -> ctpClient().complete(TypeDeleteCommand.of(type)));
    }*/
}
