package specs.paymentmethods.creditcard;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.BaseFixture;
import util.HeadlessWebDriver;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author fhaertig
 * @since 20.01.16
 */
@RunWith(ConcordionRunner.class)
public class ChargeImmediatelyWith3dsFixture extends BaseFixture {
    private static final Splitter thePaymentNamesSplitter = Splitter.on(", ");

    private static final Logger LOG = LoggerFactory.getLogger(ChargeImmediatelyWith3dsFixture.class);

    private HeadlessWebDriver webDriver;

    private Map<String, String> successUrlForPayment;

    @Before
    public void setUp() {
        webDriver = new HeadlessWebDriver();
        successUrlForPayment = new HashMap<>();
    }

    @After
    public void tearDown() {
        webDriver.quit();
    }

    public String createPayment(final String paymentName,
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
                .amountPlanned(monetaryAmount)
                .transactions(transactions)
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_CREDIT_CARD,
                        ImmutableMap.<String, Object>builder()
                                .put(CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD, pseudocardpan)
                                .put(CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage())
                                .put(CustomFieldKeys.SUCCESS_URL_FIELD, "https://www.google.de/#q=" + URLEncoder.encode(paymentName + " Success", "UTF-8"))
                                .put(CustomFieldKeys.ERROR_URL_FIELD, "https://www.google.de/#q=" + URLEncoder.encode(paymentName + " Error", "UTF-8"))
                                .put(CustomFieldKeys.CANCEL_URL_FIELD, "https://www.google.de/#q=" + URLEncoder.encode(paymentName + " Cancel", "UTF-8"))
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
        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                BaseFixture.EMPTY_STRING;
        final String amountPaid = (payment.getAmountPaid() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountPaid()) :
                BaseFixture.EMPTY_STRING;

        return ImmutableMap.<String, String> builder()
                .put("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .put("interactionCount", getInteractionRequestCount(payment, transactionId, requestType))
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("amountAuthorized", amountAuthorized)
                .put("amountPaid", amountPaid)
                .put("version", payment.getVersion().toString())
                .build();
    }

    public Map<String, String> fetchPaymentDetails(final String paymentName) throws InterruptedException, ExecutionException {
        Payment payment = fetchPaymentByLegibleName(paymentName);

        final String transactionId = getIdOfLastTransaction(payment);
        final String responseRedirectUrl = getInteractionRedirect(payment, transactionId)
                .map(i -> i.getFieldAsString(CustomFieldKeys.REDIRECT_URL_FIELD))
                .orElse(EMPTY_STRING);
        int urlTrimAt = responseRedirectUrl.contains("?") ? responseRedirectUrl.indexOf("?") : 0;

        long appointedNotificationCount = getInteractionNotificationCountOfAction(payment, "appointed");
        long paidNotificationCount = getInteractionNotificationCountOfAction(payment, "paid");

        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                "null";

        final String amountPaid = (payment.getAmountPaid() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountPaid()) :
                BaseFixture.EMPTY_STRING;

        return ImmutableMap.<String, String>builder()
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("responseRedirectUrl", responseRedirectUrl.substring(0, urlTrimAt))
                .put("amountAuthorized", amountAuthorized)
                .put("amountPaid", amountPaid)
                .put("successUrl", successUrlForPayment.getOrDefault(paymentName, EMPTY_STRING))
                .put("appointedNotificationCount", String.valueOf(appointedNotificationCount))
                .put("paidNotificationCount", String.valueOf(paidNotificationCount))
                .put("version", payment.getVersion().toString())
                .build();
    }

    public boolean executeRedirectForPayments(final String paymentNames)
            throws InterruptedException, ExecutionException {

        final ImmutableList<String> paymentNamesList = ImmutableList.copyOf(thePaymentNamesSplitter.split(paymentNames));

        for (String paymentName : paymentNamesList) {
            Payment payment = fetchPaymentByLegibleName(paymentName);
            final String transactionId = getIdOfLastTransaction(payment);
            final String responseRedirectUrl = getInteractionRedirect(payment, transactionId)
                    .map(i -> i.getFieldAsString(CustomFieldKeys.REDIRECT_URL_FIELD))
                    .orElse(EMPTY_STRING);

            successUrlForPayment.put(paymentName, getUrlAfter3dsVerification(responseRedirectUrl));
        }

        return successUrlForPayment.size() == paymentNamesList.size();
    }


    public boolean isInteractionRedirectPresent(final String paymentName) throws ExecutionException {
        Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        return getInteractionRedirect(payment, transactionId).isPresent();
    }

    protected String getUrlAfter3dsVerification(final String responseRedirectUrl) throws InterruptedException {
        if (responseRedirectUrl == null || responseRedirectUrl.isEmpty()) {
            return EMPTY_STRING;
        }

        webDriver.navigate().to(responseRedirectUrl);
        WebElement element = webDriver.findElement(By.xpath("//input[@name=\"password\"]"));
        element.sendKeys(getTestData3DsPassword());
        element.submit();
        return webDriver.getCurrentUrl();
    }

    public boolean receivedNotificationOfActionFor(final String paymentNames, final String txaction) throws InterruptedException, ExecutionException {
        final ImmutableList<String> paymentNamesList = ImmutableList.copyOf(thePaymentNamesSplitter.split(paymentNames));

        long remainingWaitTimeInMillis = PAYONE_NOTIFICATION_TIMEOUT;

        final long sleepDuration = 100L;

        long numberOfPaymentsWithNotification = countPaymentsWithNotificationOfAction(paymentNamesList, txaction);
        while ((numberOfPaymentsWithNotification != paymentNamesList.size()) && (remainingWaitTimeInMillis > 0L)) {
            Thread.sleep(sleepDuration);
            remainingWaitTimeInMillis -= sleepDuration;
            numberOfPaymentsWithNotification = countPaymentsWithNotificationOfAction(paymentNamesList, txaction);
        }

        LOG.info(String.format(
                "waited %d seconds to receive notifications of type '%s' for payments %s",
                TimeUnit.MILLISECONDS.toSeconds(PAYONE_NOTIFICATION_TIMEOUT - remainingWaitTimeInMillis),
                txaction,
                Arrays.toString(paymentNamesList.stream().map(this::getIdForLegibleName).toArray())));

        return numberOfPaymentsWithNotification == paymentNamesList.size();
    }

    public long getInteractionNotificationCountOfAction(final String paymentName, final String txaction) throws ExecutionException {
        Payment payment = fetchPaymentByLegibleName(paymentName);
        return getInteractionNotificationCountOfAction(payment, txaction);
    }
}
