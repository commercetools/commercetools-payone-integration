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
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.BaseFixture;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Jan Wolter
 */
@RunWith(ConcordionRunner.class)
public class ChargeImmediatelyWithout3dsFixture extends BaseFixture {
    private static final Splitter thePaymentNamesSplitter = Splitter.on(", ");

    private static final Logger LOG = LoggerFactory.getLogger(ChargeImmediatelyWithout3dsFixture.class);

    public String createPayment(final String paymentName,
                                final String paymentMethod,
                                final String transactionType,
                                final String centAmount,
                                final String currencyCode) throws ExecutionException, InterruptedException {

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
                .amountPlanned(monetaryAmount)
                .transactions(transactions)
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_CREDIT_CARD,
                        ImmutableMap.of(
                                CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD, getUnconfirmedVisaPseudoCardPan(),
                                CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage(),
                                CustomFieldKeys.REFERENCE_FIELD, "myGlobalKey")))
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

        final Payment payment = fetchPaymentByLegibleName(paymentName);

        final long appointedNotificationCount = getInteractionNotificationCountOfAction(payment, "appointed");
        final long paidNotificationCount = getInteractionNotificationCountOfAction(payment, "paid");

        final String transactionId = getIdOfLastTransaction(payment);
        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                BaseFixture.EMPTY_STRING;

        final String amountPaid = (payment.getAmountPaid() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountPaid()) :
                BaseFixture.EMPTY_STRING;

        return ImmutableMap.<String, String> builder()
                .put("appointedNotificationCount", Long.toString(appointedNotificationCount))
                .put("paidNotificationCount", Long.toString(paidNotificationCount))
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("amountAuthorized", amountAuthorized)
                .put("amountPaid", amountPaid)
                .put("version", payment.getVersion().toString())
                .build();
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
