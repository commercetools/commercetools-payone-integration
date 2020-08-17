package specs.paymentmethods.creditcard;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.SetCustomField;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.apache.http.HttpResponse;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.paymentmethods.BaseNotifiablePaymentFixture;
import util.WebDriver3ds;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.apache.commons.lang3.StringUtils.substringBefore;

/**
 * @author fhaertig
 * @since 10.12.15
 */
@RunWith(ConcordionRunner.class)
public class AuthorizationWith3dsFixture extends BaseNotifiablePaymentFixture {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationWith3dsFixture.class);

    private static final String baseRedirectUrl = "https://example.com/creditcard_authorization_with_verification/";

    private WebDriver3ds webDriver;

    @Before
    public void setUp() throws Exception {
        super.setUp();
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
            final String currencyCode) {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);
        final String pseudocardpan = getVerifiedVisaPseudoCardPan();

        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(paymentMethod)
                        .paymentInterface("PAYONE")
                        .build())
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_CREDIT_CARD,
                        ImmutableMap.<String, Object>builder()
                                .put(CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD, pseudocardpan)
                                .put(CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage())
                                .put(CustomFieldKeys.SUCCESS_URL_FIELD, createRedirectUrl(baseRedirectUrl, paymentName, "Success"))
                                .put(CustomFieldKeys.ERROR_URL_FIELD, createRedirectUrl(baseRedirectUrl, paymentName, "Error"))
                                .put(CustomFieldKeys.CANCEL_URL_FIELD, createRedirectUrl(baseRedirectUrl, paymentName, "Cancel"))
                                .put(CustomFieldKeys.REFERENCE_FIELD, "<placeholder>")
                                .build()))
                .build();

        final BlockingSphereClient ctpClient = ctpClient();
        final Payment payment = ctpClient.executeBlocking(PaymentCreateCommand.of(paymentDraft));
        registerPaymentWithLegibleName(paymentName, payment);

        final String orderNumber = createCartAndOrderForPayment(payment, currencyCode);

        ctpClient.executeBlocking(PaymentUpdateCommand.of(
                payment,
                ImmutableList.<UpdateActionImpl<Payment>>builder()
                        .add(AddTransaction.of(TransactionDraftBuilder.of(
                                TransactionType.valueOf(transactionType),
                                monetaryAmount,
                                ZonedDateTime.now())
                                .state(TransactionState.PENDING)
                                .build()))
                        .add(SetCustomField.ofObject(CustomFieldKeys.REFERENCE_FIELD, orderNumber))
                        .build()));

        return payment.getId();
    }

    public Map<String, String> handlePayment(final String paymentName,
                                             final String requestType) throws ExecutionException, IOException {
        final HttpResponse response = requestToHandlePaymentByLegibleName(paymentName);
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        return ImmutableMap.<String, String>builder()
                .put("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .put("interactionCount", getInteractionRequestCount(payment, transactionId, requestType))
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("version", payment.getVersion().toString())
                .build();
    }

    public Map<String, String> fetchPaymentDetails(final String paymentName) {
        final Payment payment = fetchPaymentByLegibleName(paymentName);

        final String transactionId = getIdOfLastTransaction(payment);
        final String responseRedirectUrl = Optional
            .ofNullable(payment.getCustom())
            .flatMap(customFields ->
                Optional.ofNullable(customFields.getFieldAsString(CustomFieldKeys.REDIRECT_URL_FIELD)))
            .map(url -> substringBefore(url,
                Objects.equals(payment.getPaymentMethodInfo().getMethod(), "CREDIT_CARD") ? "/redirect/" : "?"))
            .orElse(NULL_STRING);

        return ImmutableMap.<String, String>builder()
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("responseRedirectUrl", responseRedirectUrl)
                .put("version", payment.getVersion().toString())
                .build();
    }

    public Map<String, String> executeRedirectAndWaitForNotificationOfAction(final String paymentName,
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
        final long appointedNotificationCount = getTotalNotificationCountOfAction(updatedPayment, txAction);

        return ImmutableMap.<String, String>builder()
                .put("appointedNotificationCount", String.valueOf(appointedNotificationCount))
                .put("transactionState", getTransactionState(updatedPayment, transactionId))
                .put("version", updatedPayment.getVersion().toString())
                .build();
    }

    @Override
    public String fetchOrderPaymentState(final String paymentName) {
        // we keep this overriding just to easily see which test methods are run in this fixture
        return super.fetchOrderPaymentState(getIdForLegibleName(paymentName));
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
