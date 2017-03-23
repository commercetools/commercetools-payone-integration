package specs.service;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.service.PaymentService;
import com.commercetools.service.PaymentServiceImpl;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import specs.BaseFixture;

import javax.money.MonetaryAmount;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.commercetools.pspadapter.payone.util.CompletionUtil.executeBlocking;
import static specs.response.BasePaymentFixture.baseRedirectUrl;

@RunWith(ConcordionRunner.class)
public class PaymentServiceFixture extends BaseFixture {

    private PaymentService paymentService;

    private Map<String, String> paymentNameToOrderNumberMap;

    private static final String ORDER_NUMBER_SEPARATOR = "#";

    @Before
    public void setUp() throws Exception {
        super.initializeCommercetoolsClient();
        paymentService = new PaymentServiceImpl(this.ctpClient());
        paymentNameToOrderNumberMap = new HashMap<>();
    }

    public MultiValueResult createPayment(
            final String paymentName,
            final String paymentMethodInterface,
            final String paymentMethod,
            final String interfaceIdPrefix,
            final String centAmount,
            final String currencyCode) throws Exception {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);

        final String orderNumber = interfaceIdPrefix + ORDER_NUMBER_SEPARATOR + getRandomOrderNumber();

        final String successUrl = baseRedirectUrl + URLEncoder.encode(paymentName + " Success", "UTF-8");
        final String errorUrl = baseRedirectUrl + URLEncoder.encode(paymentName + " Error", "UTF-8");
        final String cancelUrl = baseRedirectUrl + URLEncoder.encode(paymentName + " Cancel", "UTF-8");
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
                                .put(CustomFieldKeys.REFERENCE_FIELD, "<placeholder>")
                                .build()))
                .build();

        // tested method: com.commercetools.service.PaymentService.createPayment()
        Payment payment = executeBlocking(paymentService.createPayment(paymentDraft));

        registerPaymentWithLegibleName(paymentName, payment);
        paymentNameToOrderNumberMap.put(paymentName, orderNumber);

        return MultiValueResult.multiValueResult()
                .with("paymentId", payment.getId())
                .with("interfaceId", payment.getInterfaceId())
                .with("paymentStatus", payment.getPaymentStatus().getInterfaceCode());
    }

    public MultiValueResult getByPaymentMethodAndInterfaceId(String paymentName, String paymentMethodInterface) {
        final String interfaceId = paymentNameToOrderNumberMap.get(paymentName);
        Optional<Payment> payment = executeBlocking(paymentService.getByPaymentMethodAndInterfaceId(paymentMethodInterface, interfaceId));

        String fetchedInterfaceId = payment.map(Payment::getInterfaceId).orElse("<UNDEFINED>");
        int endIndex = fetchedInterfaceId.indexOf(ORDER_NUMBER_SEPARATOR);
        endIndex = endIndex < 0 ? fetchedInterfaceId.length(): endIndex;

        return MultiValueResult.multiValueResult()
                .with("paymentMethod", payment.map(Payment::getPaymentMethodInfo).map(PaymentMethodInfo::getMethod).orElse("<UNDEFINED>"))
                .with("amountPlanned", payment.map(Payment::getAmountPlanned).map(currencyFormatterDe::format).orElse("<UNDEFINED>"))
                .with("interfaceIdPrefix", fetchedInterfaceId.substring(0, endIndex))
                .with("interfaceId", fetchedInterfaceId);
    }


}
