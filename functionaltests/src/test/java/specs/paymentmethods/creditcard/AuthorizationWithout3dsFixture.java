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
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.BaseFixture;
import specs.paymentmethods.BaseNotifiablePaymentFixture;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * @author fhaertig
 * @since 10.12.15
 */
@RunWith(ConcordionRunner.class)
public class AuthorizationWithout3dsFixture extends BaseNotifiablePaymentFixture {

    public String createPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode) throws ExecutionException, InterruptedException {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);
        final String pseudocardpan = getUnconfirmedVisaPseudoCardPan();

        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(paymentMethod)
                        .paymentInterface("PAYONE")
                        .build())
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_CREDIT_CARD,
                        ImmutableMap.of(
                                CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD, pseudocardpan,
                                CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage(),
                                CustomFieldKeys.REFERENCE_FIELD, "<placeholder>")))
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

    public MultiValueResult handlePayment(final String paymentName,
                                             final String requestType) throws ExecutionException, IOException {
        final HttpResponse response = requestToHandlePaymentByLegibleName(paymentName);
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                BaseFixture.EMPTY_STRING;

        return MultiValueResult.multiValueResult()
                .with("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .with("interactionCount", getInteractionRequestCount(payment, transactionId, requestType))
                .with("transactionState", getTransactionState(payment, transactionId))
                .with("amountAuthorized", amountAuthorized)
                .with("version", payment.getVersion().toString());
    }

    @Override
    public boolean receivedNotificationOfActionFor(final String paymentNames, final String txaction) throws Exception {
        // we keep this overriding just to easily see which test methods are run in this fixture
        return super.receivedNotificationOfActionFor(paymentNames, txaction);
    }

    @Override
    public String fetchOrderPaymentState(final String paymentName) {
        // we keep this overriding just to easily see which test methods are run in this fixture
        return super.fetchOrderPaymentState(getIdForLegibleName(paymentName));
    }

    public MultiValueResult fetchPaymentDetails(final String paymentName) throws InterruptedException, ExecutionException {

        Payment payment = fetchPaymentByLegibleName(paymentName);
        long appointedNotificationCount = getTotalNotificationCountOfAction(payment, "appointed");

        final String transactionId = getIdOfLastTransaction(payment);
        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                BaseFixture.EMPTY_STRING;

        return MultiValueResult.multiValueResult()
                .with("notificationCount", Long.toString(appointedNotificationCount))
                .with("transactionState", getTransactionState(payment, transactionId))
                .with("amountAuthorized", amountAuthorized)
                .with("version", payment.getVersion().toString());
    }

    public long getInteractionNotificationOfActionCount(final String paymentName, final String txaction) throws ExecutionException {
        Payment payment = fetchPaymentByLegibleName(paymentName);
        return getTotalNotificationCountOfAction(payment, txaction);
    }
}
