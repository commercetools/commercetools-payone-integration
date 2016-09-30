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
import org.concordion.api.FullOGNL;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.BaseFixture;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static specs.response.ResponsesFixture.baseRedirectUrl;

/**
 * Test Payone integration service sets "response" custom field for executed transactions as a valid JSON string
 * with values returned from payment provider.
 * "response" custom field should contain JSON string in both error and success cases:<ul>
 *     <li>in case of error: contains error description (errorcode, errormessage, customermessage)</li>
 *     <li>in case of success: contains payment type related vales (status, redirecturl, txid, userid and so on)</li>
 * </ul>
 */
@RunWith(ConcordionRunner.class)
@FullOGNL
public class JsonResponseFixture extends BaseFixture {

    public String createCardPayment(String paymentName,
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

    public String createWalletPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode,
            final String languageCode) throws ExecutionException, InterruptedException, UnsupportedEncodingException {

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
                                .put(CustomFieldKeys.LANGUAGE_CODE_FIELD, languageCode)
                                .put(CustomFieldKeys.SUCCESS_URL_FIELD, successUrl)
                                .put(CustomFieldKeys.ERROR_URL_FIELD, errorUrl)
                                .put(CustomFieldKeys.CANCEL_URL_FIELD, cancelUrl)
                                .put(CustomFieldKeys.REFERENCE_FIELD, "<placeholder>")
                                .build()))
                .build();

        Payment payment = createPaymentFromDraft(paymentName, paymentDraft, transactionType);
        return payment.getId();
    }

    /**
     * Returns
     * @param paymentName previously created payment name from HTML template
     * @return complete {@link JsonNode} with expected fields from Payone API (errorcode, errormessage, customermessage)
     * @throws ExecutionException
     * @throws IOException
     */
    public JsonNode handleErrorJsonResponse(final String paymentName) throws ExecutionException, IOException {
        return handleJsonResponse(paymentName);
    }

    /**
     * Read and map to result some values from JSON response
     * @param paymentName previously created payment name from HTML template
     * @return map with the next values:<ul>
     *     <li><i>status</i>: string status value (APPROVED, PENDING, REDIRECT, ERROR)</li>
     *     <li><i>redirectUrlAuthority</i>: expected partial URI (protocol + hostname)</li>
     *     <li><i>txidIsSet</i>: boolean value, <b>true</b> if <i>txid</i> exists and not empty in JSON response </li>
     * </ul>
     * @throws ExecutionException
     * @throws IOException
     */
    public Map<String, Object> handleSuccessJsonResponse(final String paymentName) throws ExecutionException, IOException {
        JsonNode responseNode = handleJsonResponse(paymentName);

        URL redirectUrl = new URL(responseNode.get("redirecturl").asText());

        return ImmutableMap.of(
                "status", responseNode.get("status").asText(),
                "redirectUrlAuthority", redirectUrl.getProtocol() + "://" + redirectUrl.getAuthority(),
                "txidIsSet", !responseNode.get("txid").asText().isEmpty()
        );
    }

    /**
     * Handle response by given name and parse "response" result from JSON string to {@link JsonNode} result.
     * @param paymentName previously created payment name from HTML template
     * @return JsonNode with response key-values if exists, or text node with error message otherwise
     * @throws ExecutionException
     * @throws IOException
     */
    private JsonNode handleJsonResponse(final String paymentName) throws ExecutionException, IOException {
        return handlePaymentByName(paymentName)
                .getInterfaceInteractions()
                .stream()
                .map(customFields -> customFields.getFieldAsString("response"))
                .filter(Objects::nonNull)
                .findFirst()
                .map(SphereJsonUtils::parse)
                .orElse(new TextNode("ERROR in payment transaction result: response JSON node not found"));
    }

    private Payment createPaymentFromDraft(String paymentName, PaymentDraft paymentDraft, String transactionType) {
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

    private Payment handlePaymentByName(final String paymentName) throws ExecutionException, IOException {
        requestToHandlePaymentByLegibleName(paymentName);
        return fetchPaymentByLegibleName(paymentName);
    }
}
