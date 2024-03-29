package util;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.pspadapter.payone.util.PayoneHash;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.carts.CartDraft;
import io.sphere.sdk.carts.CartDraftBuilder;
import io.sphere.sdk.carts.commands.CartCreateCommand;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.AddPayment;
import io.sphere.sdk.carts.commands.updateactions.SetBillingAddress;
import io.sphere.sdk.carts.commands.updateactions.SetShippingAddress;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.PaymentMethodInfoBuilder;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.MoneyImpl;

import javax.annotation.Nonnull;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.commercetools.util.PayoneHttpClientUtil.nameValue;
import static java.lang.String.format;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static util.PropertiesHelperUtil.getCardPan;
import static util.PropertiesHelperUtil.getClientId;
import static util.PropertiesHelperUtil.getClientSecret;
import static util.PropertiesHelperUtil.getPayoneKey;
import static util.PropertiesHelperUtil.getPayoneMerchantId;
import static util.PropertiesHelperUtil.getPayonePortalId;
import static util.PropertiesHelperUtil.getPayoneSubAccId;
import static util.PropertiesHelperUtil.getProjectKey;
import static util.PropertiesHelperUtil.getTenant;

public class PaymentRequestHelperUtil {

    public static final BlockingSphereClient CTP_CLIENT = createClient();
    public static final String URL_PATTERN = "http://localhost:%d";
    public static final String URL_START_SESSION = format("%s/%s/commercetools/start/session/", URL_PATTERN,
            getTenant());
    public static final String URL_HANDLE_PAYMENT = format("%s/%s/commercetools/handle/payments/", URL_PATTERN, getTenant());

    protected static final Random randomSource = new Random();

    private static final int CTP_REQUEST_TIMEOUT = 2;
    private static String pseudoCardPan = null;

    @Nonnull
    public static BlockingSphereClient createClient() {
        return BlockingSphereClient.of(
            SphereClientFactory.of().createClient(getProjectKey(), getClientId(), getClientSecret()),
            Duration.ofMinutes(CTP_REQUEST_TIMEOUT)
        );
    }

    @Nonnull
    public static String createPayment(
        @Nonnull final String paymentMethod,
        @Nonnull final String centAmount,
        @Nonnull final String currencyCode) {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);
        return preparePaymentWithPreauthorizedAmountAndOrder(monetaryAmount, paymentMethod);
    }

    @Nonnull
    private static MonetaryAmount createMonetaryAmountFromCent(@Nonnull final Long centAmount,
                                                        @Nonnull final String currencyCode) {
        return MoneyImpl.ofCents(centAmount, currencyCode);
    }

    @Nonnull
    private static String preparePaymentWithPreauthorizedAmountAndOrder(@Nonnull final MonetaryAmount monetaryAmount,
                                                                 @Nonnull final String paymentMethod) {
        final Map<String, Object> customFieldKeysMap = new HashMap<>();
        customFieldKeysMap.put(CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD, getUnconfirmedVisaPseudoCardPan());
        customFieldKeysMap.put(CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage());
        customFieldKeysMap.put(CustomFieldKeys.REFERENCE_FIELD, getRandomOrderNumber());
        customFieldKeysMap.put(CustomFieldKeys.SUCCESS_URL_FIELD, "https://example.com/success");
        customFieldKeysMap.put(CustomFieldKeys.ERROR_URL_FIELD, "https://example.com/error");

        final PaymentDraft paymentDraft =
            PaymentDraftBuilder.of(monetaryAmount)
                               .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                                                                          .method(paymentMethod)
                                                                          .paymentInterface("PAYONE")
                                                                          .build())
                               .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                                   CustomTypeBuilder.PAYMENT_CREDIT_CARD, customFieldKeysMap))
                               .build();

        final Payment payment = CTP_CLIENT.executeBlocking(PaymentCreateCommand.of(paymentDraft));
        final CartDraft cardDraft = CartDraftBuilder.of(Monetary.getCurrency("EUR")).build();

        CTP_CLIENT.executeBlocking(CartUpdateCommand.of(
            CTP_CLIENT.executeBlocking(CartCreateCommand.of(cardDraft)),
            Arrays.asList(
                AddPayment.of(payment),
                SetShippingAddress.of(Address.of(CountryCode.DE)),
                SetBillingAddress.of(Address.of(CountryCode.DE).withLastName("Test Buyer"))
            )));

        CTP_CLIENT.executeBlocking(PaymentUpdateCommand.of(payment, AddTransaction.of(TransactionDraftBuilder
            .of(TransactionType.AUTHORIZATION, monetaryAmount, ZonedDateTime.now())
            .state(TransactionState.PENDING)
            .build())));

        return payment.getId();
    }


    @Nonnull
    public static String createKlarnaPayment(
            @Nonnull final String paymentMethod,
            @Nonnull final String centAmount,
            @Nonnull final String currencyCode) {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);
        final Map<String, Object> customFieldKeysMap = new HashMap<>();

        customFieldKeysMap.put(CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage());
        customFieldKeysMap.put(CustomFieldKeys.REFERENCE_FIELD, getRandomOrderNumber());
        customFieldKeysMap.put(CustomFieldKeys.SUCCESS_URL_FIELD, "https://example.com/success");
        customFieldKeysMap.put(CustomFieldKeys.ERROR_URL_FIELD, "https://example.com/error");

        final PaymentDraft paymentDraft =
                PaymentDraftBuilder.of(monetaryAmount)
                        .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                                .method(paymentMethod)
                                .paymentInterface("PAYONE")
                                .build())
                        .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                                CustomTypeBuilder.PAYMENT_INVOICE_KLARNA, customFieldKeysMap))
                        .build();

        final Payment payment = CTP_CLIENT.executeBlocking(PaymentCreateCommand.of(paymentDraft));
        final CartDraft cardDraft = CartDraftBuilder.of(Monetary.getCurrency("EUR")).build();

        CTP_CLIENT.executeBlocking(CartUpdateCommand.of(
                CTP_CLIENT.executeBlocking(CartCreateCommand.of(cardDraft)),
                Arrays.asList(
                        AddPayment.of(payment),
                        SetShippingAddress.of(Address.of(CountryCode.DE)),
                        SetBillingAddress.of(Address.of(CountryCode.DE).withLastName("Test Buyer"))
                )));
        return payment.getId();
    }
    protected static String getRandomOrderNumber() {
        return String.valueOf(randomSource.nextInt() + System.nanoTime());
    }

    private static String getUnconfirmedVisaPseudoCardPan() {
        if (pseudoCardPan == null) {
            pseudoCardPan = fetchPseudoCardPan(
                getCardPan(), getPayoneMerchantId(), getPayoneSubAccId(), getPayonePortalId(), getPayoneKey());
        }
        return pseudoCardPan;
    }

    /**
     * Request a new pseudocardpan for {@code cardPan} Visa number.
     * <p>
     * <b>Note:</b> Pseudocardpan is a unique number for every merchant ID, thus ensure you tested service and these
     * integration tests use the same payone merchant credentials.
     * Verify your {@code PAYONE_MERCHANT_ID} and {@code TEST_DATA_PAYONE_MERCHANT_ID} for both applications if
     * you get "pseudocardpan is not found" response from Payone.
     *
     * @param cardPan Visa card number (expected to be test Visa number from Payone)
     * @param mid Payone Merchant Id
     * @param aid Payone Sub-Account Id
     * @param pid Payone Portal Id
     * @param key Payone Key (Access Token)
     * @return pseudocardpan string, registered in Payone merchant center
     * @throws RuntimeException if the response from Payone can't be parsed
     */
    public static String fetchPseudoCardPan(String cardPan, String mid, String aid, String pid, String key) {
        //curl --data "request=3dscheck&mid=$PAYONE_MERCHANT_ID&aid=$PAYONE_SUBACC_ID&portalid=$PAYONE_PORTAL_ID&key=$(md5 -qs $PAYONE_KEY)&mode=test&api_version=3.9&amount=2&currency=EUR&clearingtype=cc&exiturl=http://www.example.com&storecarddata=yes&cardexpiredate=2512&cardcvc2=123&cardtype=V&cardpan=<VISA_CREDIT_CARD_3DS_NUMBER>"

        String cardPanResponse = null;
        try {
            cardPanResponse = PayonePostServiceImpl.executePostRequestToString("https://api.pay1.de/post-gateway/",
                Arrays.asList(nameValue("request", "creditcardcheck"),
                    nameValue("mid", mid),
                    nameValue("aid", aid),
                    nameValue("portalid", pid),
                    nameValue("key", PayoneHash.calculate(key)),
                    nameValue("mode", "test"),
                    nameValue("api_version", "3.9"),
                    nameValue("amount", "40"),
                    nameValue("currency", "EUR"),
                    nameValue("clearingtype", "cc"),
                    nameValue("exiturl", "http://www.example.com"),
                    nameValue("storecarddata", "yes"),
                    nameValue("cardexpiredate", "2512"),
                    nameValue("cardcvc2", "123"),
                    nameValue("cardtype", "V"),
                    nameValue("cardpan", cardPan)));
        } catch (Throwable e) {
            throw new RuntimeException("Error on pseudocardpan fetch", e);
        }

        Pattern p = Pattern.compile("^.*pseudocardpan\\s*=\\s*(\\d+).*$", CASE_INSENSITIVE | DOTALL);
        Matcher m = p.matcher(cardPanResponse);

        if (!m.matches()) {
            throw new RuntimeException(format("Unexpected pseudocardpan response: %s", cardPanResponse));
        }

        return m.group(1);
    }
}
