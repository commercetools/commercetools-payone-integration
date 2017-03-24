package specs.paymentmethods.sofortueberweisung;

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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.BaseFixture;
import specs.paymentmethods.BaseNotifiablePaymentFixture;
import util.WebDriverSofortueberweisung;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;

/**
 * @author fhaertig
 * @since 22.01.16
 */
@RunWith(ConcordionRunner.class)
public class ChargeImmediatelyFixture extends BaseNotifiablePaymentFixture {

    private static final String baseRedirectUrl = "https://www.example.com/sofortueberweisung_charge_immediately/";

    private WebDriverSofortueberweisung webDriver;

    private Map<String, String> successUrlForPayment;

    private static Logger LOG = LoggerFactory.getLogger(ChargeImmediatelyFixture.class);

    @Before
    public void setUp() {
        webDriver = new WebDriverSofortueberweisung("12345", "12345");
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
                                final String currencyCode,
                                final String iban,
                                final String bic) throws ExecutionException, InterruptedException, UnsupportedEncodingException {


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
                                .put(CustomFieldKeys.IBAN_FIELD, iban)
                                .put(CustomFieldKeys.BIC_FIELD, bic)
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
        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                BaseFixture.EMPTY_STRING;
        final String amountPaid = (payment.getAmountPaid() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountPaid()) :
                BaseFixture.EMPTY_STRING;

        return ImmutableMap.<String, String>builder()
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
                getTotalNotificationCountOfAction(payment, "appointed");
        final long paidNotificationCount = getTotalNotificationCountOfAction(payment, "paid");

        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                NULL_STRING;

        final String amountPaid = (payment.getAmountPaid() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountPaid()) :
                BaseFixture.EMPTY_STRING;

        return ImmutableMap.<String, String>builder()
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("responseRedirectUrlStart", responseRedirectUrl.substring(0, urlTrimAt))
                .put("successUrl", successUrlForPayment.getOrDefault(paymentName, EMPTY_STRING))
                .put("amountAuthorized", amountAuthorized)
                .put("amountPaid", amountPaid)
                .put("appointedNotificationCount", String.valueOf(appointedNotificationCount))
                .put("paidNotificationCount", String.valueOf(paidNotificationCount))
                .put("version", payment.getVersion().toString())
                .build();
    }

    public boolean executeRedirectForPayments(final String paymentNames) throws ExecutionException {
        final Collection<String> paymentNamesList = ImmutableList.copyOf(thePaymentNamesSplitter.split(paymentNames));

        paymentNamesList.forEach(paymentName -> {
            final Payment payment = fetchPaymentByLegibleName(paymentName);
            try {
                Optional.ofNullable(payment.getCustom())
                        .map(customFields -> customFields.getFieldAsString(CustomFieldKeys.REDIRECT_URL_FIELD))
                        .map(redirectCustomField -> webDriver.executeSofortueberweisungRedirect(redirectCustomField, getTestDataSwBankTransferIban())
                                .replace(baseRedirectUrl, "[...]"))
                        .ifPresent(successUrl -> successUrlForPayment.put(paymentName, successUrl));
            } catch (Exception e) {
                LOG.error("Error executing redirect for Sofortüberweisung Charge Immediate fixture", e);
            }

        });

        return successUrlForPayment.size() == paymentNamesList.size();
    }

    @Override
    public boolean receivedNotificationOfActionFor(final String paymentNames, final String txaction) throws Exception {
        // validate the payments were successfully processed in previous executeRedirectForPayments() call.
        // otherwise return false instantly
        String unconfirmedPayments = stream(thePaymentNamesSplitter.split(paymentNames).spliterator(), false)
                .filter(paymentName -> !successUrlForPayment.containsKey(paymentName))
                .collect(Collectors.joining(", "));

        if (StringUtils.isNotBlank(unconfirmedPayments)) {
            LOG.error("[{}] payments are not re-directed - the notifications [{}] won't come",
                    unconfirmedPayments, txaction);
            return false;
        }

        return super.receivedNotificationOfActionFor(paymentNames, txaction);
    }

    public boolean isInteractionRedirectPresent(final String paymentName) throws ExecutionException {
        Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        return getInteractionRedirect(payment, transactionId).isPresent();
    }
}
