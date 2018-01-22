package specs.initialPendingMigration;

import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.SetCustomField;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.apache.http.HttpResponse;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.paymentmethods.BaseNotifiablePaymentFixture;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.commercetools.pspadapter.payone.util.PayoneConstants.PAYONE;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

@RunWith(ConcordionRunner.class)
public class InitialPendingMigrationFixture extends BaseNotifiablePaymentFixture {

    public String createPaymentCreditCard(
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

    public String createPaymentPaypal(
            String paymentName,
            String paymentMethod,
            String paymentCustomType,
            String transactionType,
            String transactionState,
            String centAmount,
            String currencyCode) throws Exception {

        String successUrl = baseRedirectUrl + URLEncoder.encode(paymentName + " Success", "UTF-8");
        String errorUrl = baseRedirectUrl + URLEncoder.encode(paymentName + " Error", "UTF-8");
        String cancelUrl = baseRedirectUrl + URLEncoder.encode(paymentName + " Cancel", "UTF-8");

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

        return MultiValueResult.multiValueResult()
                .with("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .with("interactionCount", getInteractionRequestCount(payment, transactionId, requestType))
                .with("transactionState", getTransactionState(payment, transactionId))
                .with("interfaceStatusCode", payment.getPaymentStatus().getInterfaceCode())
                .with("version", payment.getVersion().toString());
    }

}
