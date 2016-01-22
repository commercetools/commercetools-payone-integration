package specs.paymentmethods.creditcard;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.BaseFixture;
import util.WebDriver3ds;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * @author fhaertig
 * @since 10.12.15
 */
@RunWith(ConcordionRunner.class)
public class AuthorizationWith3dsFixture extends BaseFixture {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationWith3dsFixture.class);

    private static final String baseRedirectUrl = "http://dev.commercetools.com/search.html?stp=1&stq=";

    private WebDriver3ds webDriver;

    @Before
    public void setUp() {
        webDriver = new WebDriver3ds();
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
        final String pseudocardpan = getVerifiedVisaPseudoCardPan();

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
                        CustomTypeBuilder.PAYMENT_CREDIT_CARD,
                        ImmutableMap.<String, Object>builder()
                                .put(CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD, pseudocardpan)
                                .put(CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage())
                                .put(CustomFieldKeys.SUCCESS_URL_FIELD, baseRedirectUrl + URLEncoder.encode(paymentName + " Success", "UTF-8"))
                                .put(CustomFieldKeys.ERROR_URL_FIELD, baseRedirectUrl + URLEncoder.encode(paymentName + " Error", "UTF-8"))
                                .put(CustomFieldKeys.CANCEL_URL_FIELD, baseRedirectUrl + URLEncoder.encode(paymentName + " Cancel", "UTF-8"))
                                .put(CustomFieldKeys.REFERENCE_FIELD, "myGlobalKey")
                                .build()))
                .build();

        final BlockingClient ctpClient = ctpClient();
        final Payment payment = ctpClient.complete(PaymentCreateCommand.of(paymentDraft));
        registerPaymentWithLegibleName(paymentName, payment);

        createCartAndOrderForPayment(payment, currencyCode);

        return payment.getId();
    }

    public Map<String, String> handlePayment(final String paymentName,
                                             final String requestType) throws ExecutionException, IOException {
        final HttpResponse response = requestToHandlePaymentByLegibleName(paymentName);
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        return ImmutableMap.<String, String> builder()
                .put("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .put("interactionCount", getInteractionRequestCount(payment, transactionId, requestType))
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("version", payment.getVersion().toString())
                .build();
    }

    public Map<String, String> fetchPaymentDetails(final String paymentName)
            throws InterruptedException, ExecutionException {
        final Payment payment = fetchPaymentByLegibleName(paymentName);

        final String transactionId = getIdOfLastTransaction(payment);
        final String responseRedirectUrl = Optional.ofNullable(payment.getCustom())
                .flatMap(customFields ->
                        Optional.ofNullable(customFields.getFieldAsString(CustomFieldKeys.REDIRECT_URL_FIELD)))
                .orElse(NULL_STRING);

        final int urlTrimAt = responseRedirectUrl.contains("?") ? responseRedirectUrl.indexOf("?") : 0;

        return ImmutableMap.<String, String>builder()
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("responseRedirectUrl", responseRedirectUrl.substring(0, urlTrimAt))
                .put("version", payment.getVersion().toString())
                .build();
    }

    public  Map<String, String> executeRedirectAndWaitForNotificationOfAction(final String paymentName,
                                                                              final String txAction)
            throws InterruptedException, ExecutionException {

        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String redirectUrlFieldName = CustomFieldKeys.REDIRECT_URL_FIELD;
        final String responseRedirectUrl = Optional.ofNullable(payment.getCustom())
                .flatMap(customFields -> Optional.ofNullable(customFields.getFieldAsString(redirectUrlFieldName)))
                .orElseThrow(() -> new IllegalStateException(
                        "No custom field \"" + redirectUrlFieldName + "\" found for Payment: " + paymentName));

        final String successUrl = getUrlAfter3dsVerification(responseRedirectUrl).replace(baseRedirectUrl, "[...]");

        //wait just a little until notification was processed (is triggered immediately after verification)
        Thread.sleep(100);

        return ImmutableMap.<String, String>builder()
                .putAll(fetchPaymentDetailsAfterAppointedNotification(payment, txAction))
                .put("successUrl", successUrl)
                .build();
    }

    private Map<String, String> fetchPaymentDetailsAfterAppointedNotification(final Payment payment,
                                                                              final String txAction)
            throws ExecutionException {
        final String transactionId = getIdOfLastTransaction(payment);
        final Payment updatedPayment = fetchPaymentById(payment.getId());
        final long appointedNotificationCount = getInteractionNotificationCountOfAction(updatedPayment, txAction);

        final String amountAuthorized = (updatedPayment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(updatedPayment.getAmountAuthorized()) :
                BaseFixture.EMPTY_STRING;

        return ImmutableMap.<String, String> builder()
                .put("appointedNotificationCount", String.valueOf(appointedNotificationCount))
                .put("transactionState", getTransactionState(updatedPayment, transactionId))
                .put("amountAuthorized", amountAuthorized)
                .put("version", updatedPayment.getVersion().toString())
                .build();
    }


    public boolean isInteractionRedirectPresent(final String paymentName) throws ExecutionException {
        Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        return getInteractionRedirect(payment, transactionId).isPresent();
    }

    protected String getUrlAfter3dsVerification(final String responseRedirectUrl) {
        if (responseRedirectUrl == null || responseRedirectUrl.isEmpty()) {
            return EMPTY_STRING;
        }

        return webDriver.execute3dsRedirectWithPassword(responseRedirectUrl, getTestData3DsPassword());
    }
}