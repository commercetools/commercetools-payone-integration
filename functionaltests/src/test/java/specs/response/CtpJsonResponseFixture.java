package specs.response;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.PaymentMethodInfoBuilder;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.concordion.api.FullOGNL;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.*;

/**
 * Test Payone integration service sets "response" custom field for executed transactions as a valid JSON string
 * with values returned from payment provider.
 * "response" custom field should contain JSON string in both error and success cases:<ul>
 * <li>in case of error: contains error description (errorcode, errormessage, customermessage)</li>
 * <li>in case of success: contains payment type related vales (status, redirecturl, txid, userid and so on)</li>
 * </ul>
 */
@RunWith(ConcordionRunner.class)
@FullOGNL
public class CtpJsonResponseFixture extends BasePaymentFixture {

    public static final String baseRedirectUrl = "https://www.example.com/json_response/";

    public String createCardPayment(String paymentName,
                                    String paymentMethod,
                                    String transactionType,
                                    String centAmount,
                                    String currencyCode,
                                    String languageCode) throws Exception {

        return createAndSaveCardPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode, languageCode)
                .getId();
    }

    public String createWalletPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode,
            final String languageCode) {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);

        final String successUrl = createRedirectUrl(baseRedirectUrl, paymentName, "Success");
        final String errorUrl = createRedirectUrl(baseRedirectUrl, paymentName, "Error");
        final String cancelUrl = createRedirectUrl(baseRedirectUrl, paymentName, "Cancel");
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

        Payment payment = createPaymentCartOrderFromDraft(paymentName, paymentDraft, transactionType);
        return payment.getId();
    }

    /**
     * Returns {@link JsonNode} with key-value String map from Payone response.
     * @param paymentName previously created payment name from HTML template
     * @return complete {@link JsonNode} with expected fields from Payone API (errorcode, errormessage, customermessage)
     * @throws ExecutionException
     * @throws IOException
     */
    public MultiValueResult handleErrorJsonResponse(final String paymentName) throws ExecutionException, IOException {
        JsonNode jsonNode = handleJsonResponse(paymentName);
        return MultiValueResult.multiValueResult()
                .with(STATUS, jsonNode.get(STATUS).textValue())
                .with(ERROR_CODE, jsonNode.get(ERROR_CODE).textValue())
                .with(ERROR_MESSAGE, jsonNode.get(ERROR_MESSAGE).textValue())
                .with(CUSTOMER_MESSAGE, jsonNode.get(CUSTOMER_MESSAGE).textValue());
    }

    /**
     * Read and map to result some values from JSON response
     * @param paymentName previously created payment name from HTML template
     * @return map with the next values:<ul>
     *     <li><i>status</i>: string status value (APPROVED, PENDING, REDIRECT, ERROR)</li>
     *     <li><i>redirectUrlAuthority</i>: expected partial URI (protocol + hostname)</li>
     *     <li><i>txidIsSet</i>: boolean value, <b>true</b> if <i>txid</i> exists and not empty in JSON response </li>
     * </ul>
     */
    public MultiValueResult handleSuccessJsonResponse(final String paymentName) throws ExecutionException, IOException {
        JsonNode responseNode = handleJsonResponse(paymentName);

        URL redirectUrl = new URL(responseNode.get("redirecturl").asText());

        return MultiValueResult.multiValueResult()
                .with(STATUS, responseNode.get(STATUS).asText())
                .with("redirectUrlAuthority", redirectUrl.getProtocol() + "://" + redirectUrl.getAuthority())
                .with("txidIsSet", !responseNode.get(TXID).asText().isEmpty());
    }
}
