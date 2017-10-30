package specs.paymentmethods.postfinance;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.base.Splitter;
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
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.BaseFixture;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author mht@dotsource.de
 */
@RunWith(ConcordionRunner.class)
public class ChargeImmediatelyFixture extends BaseFixture {
    private static final Splitter thePaymentNamesSplitter = Splitter.on(", ");

    private static final String baseRedirectUrl = "https://www.example.com/postfinance_charge_immediately/";

    private static final Logger LOG = LoggerFactory.getLogger(ChargeImmediatelyFixture.class);

    private Map<String, String> successUrlForPayment;

    @Before
    public void setUp() {
        successUrlForPayment = new HashMap<>();
    }


    public String createPayment(final String paymentName,
                                final String paymentMethod,
                                final String transactionType,
                                final String centAmount,
                                final String currencyCode) throws ExecutionException, InterruptedException, UnsupportedEncodingException {


        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);

        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(paymentMethod)
                        .paymentInterface("PAYONE")
                        .build())
                .amountPlanned(monetaryAmount)
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_BANK_TRANSFER,
                        ImmutableMap.<String, Object>builder()
                                .put(CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage())
                                .put(CustomFieldKeys.SUCCESS_URL_FIELD, baseRedirectUrl + (paymentName + " Success").replace(" ", "-"))
                                .put(CustomFieldKeys.ERROR_URL_FIELD, baseRedirectUrl + (paymentName + " Error").replace(" ", "-"))
                                .put(CustomFieldKeys.CANCEL_URL_FIELD, baseRedirectUrl + (paymentName + " Cancel").replace(" ", "-"))
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
        final String transactionId = getIdOfFirstTransaction(payment);

        return ImmutableMap.<String, String> builder()
                .put("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .put("interactionCount", getInteractionRequestCountOverAllTransactions(payment, requestType))
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("version", payment.getVersion().toString())
                .build();
    }
}
