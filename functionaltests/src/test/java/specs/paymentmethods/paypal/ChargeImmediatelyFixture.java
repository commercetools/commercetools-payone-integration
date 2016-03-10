package specs.paymentmethods.paypal;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.BaseFixture;
import specs.paymentmethods.creditcard.ChargeImmediatelyWith3dsFixture;
import util.WebDriverPaypal;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Jan Wolter
 */
@RunWith(ConcordionRunner.class)
public class ChargeImmediatelyFixture extends BaseFixture {

    private static final String baseRedirectUrl = "https://github.com/sphereio/sphere-jvm-sdk/search?q=";

    private static final Splitter thePaymentNamesSplitter = Splitter.on(", ");

    private static final Logger LOG = LoggerFactory.getLogger(ChargeImmediatelyWith3dsFixture.class);

    private final Map<String, String> successUrlForPayment = new HashMap<>();

    public Map<String, String> createPayment(final String paymentName,
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
                .amountPlanned(monetaryAmount)
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
        final Payment payment = ctpClient.complete(PaymentCreateCommand.of(paymentDraft));
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
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfFirstTransaction(payment);
        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                BaseFixture.EMPTY_STRING;
        final String amountPaid = (payment.getAmountPaid() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountPaid()) :
                BaseFixture.EMPTY_STRING;

        return ImmutableMap.<String, String> builder()
                .put("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .put("interactionCount", getInteractionRequestCountOverAllTransactions(payment, requestType))
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("amountAuthorized", amountAuthorized)
                .put("amountPaid", amountPaid)
                .put("version", payment.getVersion().toString())
                .build();
    }

    public Map<String, String> fetchPaymentDetails(final String paymentName)
            throws InterruptedException, ExecutionException {
        final Payment payment = fetchPaymentByLegibleName(paymentName);

        final String transactionId = getIdOfFirstTransaction(payment);
        final String responseRedirectUrl = Optional.ofNullable(payment.getCustom())
                .flatMap(customFields ->
                        Optional.ofNullable(customFields.getFieldAsString(CustomFieldKeys.REDIRECT_URL_FIELD)))
                .orElse(NULL_STRING);

        final int urlTrimAt = responseRedirectUrl.contains("?") ? responseRedirectUrl.indexOf("?") : 0;

        final long appointedNotificationCount =
                getInteractionNotificationCountOfAction(payment, "appointed", transactionId);

        final long paidNotificationCount = getInteractionNotificationCountOfAction(payment, "paid", transactionId);

        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                NULL_STRING;

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

    public boolean executeRedirectForPayments(final String paymentNames) throws ExecutionException {
        final Collection<String> paymentNamesList = ImmutableList.copyOf(thePaymentNamesSplitter.split(paymentNames));

        paymentNamesList.forEach(paymentName -> {
            final Payment payment = fetchPaymentByLegibleName(paymentName);
            final Optional<String> responseRedirectUrl = Optional.ofNullable(payment.getCustom())
                    .flatMap(customFields ->
                            Optional.ofNullable(customFields.getFieldAsString(CustomFieldKeys.REDIRECT_URL_FIELD)));

            if (responseRedirectUrl.isPresent()) {
                //need to create new webDriver for each payment because of Paypal session handling
                WebDriverPaypal webDriver = new WebDriverPaypal();
                final String successUrl =
                        webDriver.doLoginAndConfirmation(responseRedirectUrl.get(), getTestDataPaypalCharge())
                                .replace(baseRedirectUrl, "[...]");
                webDriver.quit();

                successUrlForPayment.put(paymentName, successUrl);
            }
        });

        return successUrlForPayment.size() == paymentNamesList.size();
    }

    public boolean receivedNotificationOfActionFor(final String paymentNames, final String txaction) throws InterruptedException, ExecutionException {
        final ImmutableList<String> paymentNamesList = ImmutableList.copyOf(thePaymentNamesSplitter.split(paymentNames));

        int notificationsToReceive = paymentNamesList.size();
        for (String paymentName : paymentNamesList) {
            //quick way to check if it is neccessary to wait for notifications
            // (e.g. if selenium produced errors the success url may not be as expected and no notifications will be received for this payment)
            if (!successUrlForPayment.get(paymentName).toLowerCase().contains("success")) {
                notificationsToReceive -= 1;
            }
        }

        long remainingWaitTimeInMillis = PAYONE_NOTIFICATION_TIMEOUT;

        final long sleepDuration = 100L;


        long numberOfPaymentsWithNotification = countPaymentsWithNotificationOfAction(paymentNamesList, txaction);
        while ((numberOfPaymentsWithNotification != notificationsToReceive) && (remainingWaitTimeInMillis > 0L)) {
            Thread.sleep(sleepDuration);
            remainingWaitTimeInMillis -= sleepDuration;
            numberOfPaymentsWithNotification = countPaymentsWithNotificationOfAction(paymentNamesList, txaction);
            if (remainingWaitTimeInMillis == TimeUnit.MINUTES.toMillis(4)
                    || remainingWaitTimeInMillis == TimeUnit.MINUTES.toMillis(2)) {
                LOG.info("Waiting for " + txaction + " notifications in PaypalChargedImmediatelyFixture takes longer than usual.");
            }
        }

        LOG.info(String.format(
                "waited %d seconds to receive notifications of type '%s' for payments %s",
                TimeUnit.MILLISECONDS.toSeconds(PAYONE_NOTIFICATION_TIMEOUT - remainingWaitTimeInMillis),
                txaction,
                Arrays.toString(paymentNamesList.stream().map(this::getIdForLegibleName).toArray())));

        return numberOfPaymentsWithNotification == paymentNamesList.size();
    }

    public boolean isInteractionRedirectPresent(final String paymentName) throws ExecutionException {
        Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        return getInteractionRedirect(payment, transactionId).isPresent();
    }

}
