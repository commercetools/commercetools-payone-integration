package specs.service;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.service.PaymentService;
import com.commercetools.service.PaymentServiceImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceCode;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceText;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.concordion.api.FullOGNL;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import specs.BaseFixture;

import javax.money.MonetaryAmount;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.*;
import static com.commercetools.pspadapter.payone.util.CompletionUtil.executeBlocking;
import static java.lang.String.format;

@RunWith(ConcordionRunner.class)
@FullOGNL // to support long arguments list for #updatePayment()
public class PaymentServiceFixture extends BaseFixture {

    public static final String baseRedirectUrl = "https://www.example.com/payment_service/";

    private PaymentService paymentService;

    private Map<String, String> paymentNameToOrderNumberMap;

    private static final String ORDER_NUMBER_SEPARATOR = "#";

    @Before
    public void setUp() throws Exception {
        paymentService = new PaymentServiceImpl(this.ctpClient());
        paymentNameToOrderNumberMap = new HashMap<>();
    }

    /**
     * Tested method: {@link PaymentService#createPayment(io.sphere.sdk.payments.PaymentDraft)}
     *
     * @param paymentName            test specific unique payment name
     * @param paymentMethodInterface payment provider name, like PAYONE
     * @param paymentMethod          method name constant, here only WALLET-PAYPAL is supported so far
     * @param interfaceIdPrefix      this prefix is added to auto-generated order number to verify in the next steps
     * @param centAmount             payment amount
     * @param currencyCode           payment currency
     * @return {@link MultiValueResult} with <i>paymentId</i>, <i>interfaceId</i>, <i>paymentStatus</i>.
     * @throws Exception
     */
    public MultiValueResult createPayment(
            final String paymentName,
            final String paymentMethodInterface,
            final String paymentMethod,
            final String interfaceIdPrefix,
            final String centAmount,
            final String currencyCode) throws Exception {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);

        final String orderNumber = interfaceIdPrefix + ORDER_NUMBER_SEPARATOR + getRandomOrderNumber();

        final String successUrl = createRedirectUrl(baseRedirectUrl, paymentName, "Success");
        final String errorUrl = createRedirectUrl(baseRedirectUrl, paymentName, "Error");
        final String cancelUrl = createRedirectUrl(baseRedirectUrl, paymentName, "Cancel");
        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(paymentMethod)
                        .paymentInterface(paymentMethodInterface)
                        .build())
                .amountPlanned(monetaryAmount)
                .interfaceId(orderNumber)
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_WALLET,
                        ImmutableMap.<String, Object>builder()
                                .put(CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage())
                                .put(CustomFieldKeys.SUCCESS_URL_FIELD, successUrl)
                                .put(CustomFieldKeys.ERROR_URL_FIELD, errorUrl)
                                .put(CustomFieldKeys.CANCEL_URL_FIELD, cancelUrl)
                                .put(CustomFieldKeys.REFERENCE_FIELD, orderNumber)
                                .build()))
                .build();

        // tested method: com.commercetools.service.PaymentService.createPayment()
        Payment payment = executeBlocking(paymentService.createPayment(paymentDraft));

        registerPaymentWithLegibleName(paymentName, payment);
        paymentNameToOrderNumberMap.put(paymentName, orderNumber);

        return MultiValueResult.multiValueResult()
                .with("paymentId", payment.getId())
                .with("interfaceId", payment.getInterfaceId())
                .with("paymentStatus", payment.getPaymentStatus().getInterfaceCode())
                .with("version", payment.getVersion());
    }

    /**
     * Test {@link PaymentService#getByPaymentMethodAndInterfaceId(java.lang.String, java.lang.String)}
     *
     * @param paymentName            test specific unique payment name, which was used in
     *                               {@link #createPayment(String, String, String, String, String, String)} above
     * @param paymentMethodInterface payment provider name, like PAYONE
     * @return {@link MultiValueResult} with <i>paymentMethod</i>, <i>amountPlanned</i>, <i>interfaceIdPrefix</i>,
     * <i>interfaceId</i>
     */
    public MultiValueResult getByPaymentMethodAndInterfaceId(String paymentName, String paymentMethodInterface) {
        final String interfaceId = paymentNameToOrderNumberMap.get(paymentName);
        Optional<Payment> payment = executeBlocking(paymentService.getByPaymentMethodAndInterfaceId(paymentMethodInterface, interfaceId));

        String fetchedInterfaceId = payment.map(Payment::getInterfaceId).orElse("<UNDEFINED>");
        int endIndex = fetchedInterfaceId.indexOf(ORDER_NUMBER_SEPARATOR);
        endIndex = endIndex < 0 ? fetchedInterfaceId.length() : endIndex;

        return MultiValueResult.multiValueResult()
                .with("paymentMethod", payment.map(Payment::getPaymentMethodInfo).map(PaymentMethodInfo::getMethod).orElse("<UNDEFINED>"))
                .with("amountPlanned", payment.map(Payment::getAmountPlanned).map(currencyFormatterDe::format).orElse("<UNDEFINED>"))
                .with("interfaceIdPrefix", fetchedInterfaceId.substring(0, endIndex))
                .with("interfaceId", fetchedInterfaceId);
    }

    /**
     * Test {@link PaymentService#updatePayment(io.sphere.sdk.payments.Payment, java.util.List)}
     */
    public MultiValueResult updatePayment(String paymentName, String statusCode, String statusText,
                                          String sequenceNumber, String txAction, String notificationText) {
        final Payment payment = fetchPaymentByLegibleName(paymentName);

        ImmutableList<UpdateAction<Payment>> actions = ImmutableList.of(
                SetStatusInterfaceCode.of(statusCode),
                SetStatusInterfaceText.of(statusText),
                AddInterfaceInteraction.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                        ImmutableMap.of(
                                TIMESTAMP_FIELD, ZonedDateTime.now(),
                                SEQUENCE_NUMBER_FIELD, sequenceNumber,
                                TX_ACTION_FIELD, txAction,
                                NOTIFICATION_FIELD, notificationText)));

        Payment updatedPayment = executeBlocking(paymentService.updatePayment(payment, actions));

        return MultiValueResult.multiValueResult()
                .with("version", updatedPayment.getVersion());
    }

    /**
     * Test the previous call of {@link #updatePayment(String, String, String, String, String, String)}
     * was successful.
     *
     * @param paymentName            test specific unique payment name
     * @param paymentMethodInterface payment provider name, like PAYONE
     * @return set of the same values as set in the {@link #updatePayment(String, String, String, String, String, String)}
     */
    public MultiValueResult verifyUpdatedPayment(String paymentName, String paymentMethodInterface) {
        final String interfaceId = paymentNameToOrderNumberMap.get(paymentName);
        Payment payment = executeBlocking(paymentService.getByPaymentMethodAndInterfaceId(paymentMethodInterface, interfaceId))
                .orElseThrow(() -> new RuntimeException(format("Payment for name [%s] and method interface [%s] not found",
                        paymentName, paymentMethodInterface)));

        if (payment.getInterfaceInteractions() == null || payment.getInterfaceInteractions().size() < 1) {
            throw new RuntimeException("Payment interface interactions list is empty");
        }

        CustomFields interfaceInteractions = payment.getInterfaceInteractions().get(0);

        return MultiValueResult.multiValueResult()
                .with("statusCode", payment.getPaymentStatus().getInterfaceCode())
                .with("statusText", payment.getPaymentStatus().getInterfaceText())
                .with("sequenceNumber", interfaceInteractions.getFieldAsString(SEQUENCE_NUMBER_FIELD))
                .with("txAction", interfaceInteractions.getFieldAsString(TX_ACTION_FIELD))
                .with("notificationText", interfaceInteractions.getFieldAsString(NOTIFICATION_FIELD));
    }


}
