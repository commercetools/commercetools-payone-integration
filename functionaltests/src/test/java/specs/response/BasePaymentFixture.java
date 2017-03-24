package specs.response;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.SetCustomField;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.BaseFixture;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static io.sphere.sdk.payments.TransactionState.FAILURE;
import static java.lang.String.format;

/**
 * Base class to create and handle test payments for Payone.
 */
public class BasePaymentFixture extends BaseFixture {

    private static final Logger LOG = LoggerFactory.getLogger(BasePaymentFixture.class);

    public static final String baseRedirectUrl = "https://www.example.com/sofortueberweisung_charge_immediately/";

    /**
     * Creates credit card payment (PAYMENT_CREDIT_CARD) and saves it to commercetools service. Also created payment is
     * saved in {@code #payments} map and may be reused in further sequental tests.
     * @param paymentName Unique payment name to use inside this test fixture
     * @param paymentMethod see {@link com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod}
     * @param transactionType  see {@link TransactionType}
     * @param centAmount amount to process (charge, authorize, refund etc)
     * @param currencyCode 3 letters currency code. The tested product should have this currency type, otherwise the
     *                     payment won't be created.
     * @param languageCode 2 letters ISO 639 code
     * @return String id of created payment.
     */
    protected String createAndSaveCardPayment(String paymentName,
                                              String paymentMethod,
                                              String transactionType,
                                              String centAmount,
                                              String currencyCode,
                                              String languageCode) throws ExecutionException, InterruptedException, UnsupportedEncodingException {
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
                                .put(CustomFieldKeys.LANGUAGE_CODE_FIELD, languageCode)
                                .put(CustomFieldKeys.SUCCESS_URL_FIELD, baseRedirectUrl + URLEncoder.encode(paymentName + " Success", "UTF-8"))
                                .put(CustomFieldKeys.ERROR_URL_FIELD, baseRedirectUrl + URLEncoder.encode(paymentName + " Error", "UTF-8"))
                                .put(CustomFieldKeys.CANCEL_URL_FIELD, baseRedirectUrl + URLEncoder.encode(paymentName + " Cancel", "UTF-8"))
                                .put(CustomFieldKeys.REFERENCE_FIELD, "<placeholder>")
                                .build()))
                .build();

        Payment payment = createPaymentFromDraft(paymentName, paymentDraft, transactionType);
        return payment.getId();
    }

    /**
     * Creates and saves payment from supplied draft. The method may be reused for different payment methods and types
     * as soon as they are already set in {@code paymentDraft}.
     * @param paymentName Unique payment name to use inside this test fixture
     * @param paymentDraft {@link PaymentDraft} from which to create and save payment
     * @param transactionType see {@link TransactionType}
     * @return Instance of created and saved payment object
     */
    protected Payment createPaymentFromDraft(String paymentName, PaymentDraft paymentDraft, String transactionType) {
        final BlockingSphereClient ctpClient = ctpClient();
        final Payment payment = ctpClient.executeBlocking(PaymentCreateCommand.of(paymentDraft));

        registerPaymentWithLegibleName(paymentName, payment);

        final String orderNumber = createCartAndOrderForPayment(payment, paymentDraft.getAmountPlanned().getCurrency().getCurrencyCode());

        ctpClient.executeBlocking(PaymentUpdateCommand.of(
                payment,
                ImmutableList.<UpdateActionImpl<Payment>>builder()
                        .add(AddTransaction.of(TransactionDraftBuilder.of(
                                TransactionType.valueOf(transactionType),
                                paymentDraft.getAmountPlanned(),
                                ZonedDateTime.now())
                                .state(TransactionState.PENDING)
                                .build()))
                        .add(SetCustomField.ofObject(CustomFieldKeys.REFERENCE_FIELD, orderNumber))
                        .build()));

        return payment;
    }

    /**
     * Handle response by given name and parse "response" result from JSON string to {@link JsonNode} result.
     * @param paymentName previously created payment name from HTML template
     * @return JsonNode with response key-values if exists, or text node with error message otherwise
     */
    protected JsonNode handleJsonResponse(final String paymentName) throws ExecutionException, IOException {
        return handlePaymentByName(paymentName)
                .getInterfaceInteractions()
                .stream()
                .map(customFields -> customFields.getFieldAsString("response"))
                .filter(Objects::nonNull)
                .findFirst()
                .map(SphereJsonUtils::parse)
                .orElse(new TextNode("ERROR in payment transaction result: response JSON node not found"));
    }

    /**
     * Check all the payments from {@code paymentNamesList} have non-FAILURE status. If at least one is failed -
     * {@link IllegalStateException} is thrown.
     * @param paymentNamesList names of payments to check
     * @throws IllegalStateException if some payments are failed.
     */
    protected void validatePaymentsNotFailed(final List<String> paymentNamesList) throws IllegalStateException {
        // since fetchPaymentByLegibleName() is a slow blocking operation - make the stream parallel
        paymentNamesList.parallelStream().forEach(paymentName ->{
            Payment payment = fetchPaymentByLegibleName(paymentName);
            List<Transaction> transactions = payment.getTransactions();
            TransactionState lastTransactionState = transactions.size() > 0 ? transactions.get(transactions.size() - 1).getState() : null;
            if (FAILURE.equals(lastTransactionState)) {
                throw new IllegalStateException(format("Payment [%s] transaction is FAILURE, payment status is [%s]",
                        payment.getId(), payment.getPaymentStatus().getInterfaceCode()));
            }
        });
    }

    /**
     * Process payment on Payone service.
     * @param paymentName Previously used unique payment name
     * @return processed payment object
     */
    private Payment handlePaymentByName(final String paymentName) throws ExecutionException, IOException {
        requestToHandlePaymentByLegibleName(paymentName);
        return fetchPaymentByLegibleName(paymentName);
    }

}
