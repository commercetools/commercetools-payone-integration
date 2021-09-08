package com.commercetools.main;

import com.commercetools.Main;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.carts.CartDraft;
import io.sphere.sdk.carts.CartDraftBuilder;
import io.sphere.sdk.carts.commands.CartCreateCommand;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.AddPayment;
import io.sphere.sdk.carts.commands.updateactions.SetBillingAddress;
import io.sphere.sdk.carts.commands.updateactions.SetShippingAddress;
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
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.MoneyImpl;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import static com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl.executeGetRequest;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus.APPROVED;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus.REDIRECT;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static util.PaymentRequestHelperUtil.CTP_CLIENT;
import static util.PaymentRequestHelperUtil.URL_HANDLE_PAYMENT;
import static util.PaymentRequestHelperUtil.createPayment;
import static util.PropertiesHelperUtil.getClientId;
import static util.PropertiesHelperUtil.getClientSecret;
import static util.PropertiesHelperUtil.getPayoneKey;
import static util.PropertiesHelperUtil.getPayoneMerchantId;
import static util.PropertiesHelperUtil.getPayonePortalId;
import static util.PropertiesHelperUtil.getPayoneSubAccId;
import static util.PropertiesHelperUtil.getProjectKey;
import static util.PropertiesHelperUtil.getTenant;

public class BasicPaymentRequestWorkflow {

    public static final int DEFAULT_PORT = 8080;
    private static final Map<String, String> testInternalProperties = new HashMap<>();

    @BeforeClass
    public static void setUp() {
        testInternalProperties.put("TENANTS", getTenant());

        testInternalProperties.put("TEST_DATA_CT_PROJECT_KEY", getProjectKey());
        testInternalProperties.put("TEST_DATA_CT_CLIENT_ID", getClientId());
        testInternalProperties.put("TEST_DATA_CT_CLIENT_SECRET", getClientSecret());
        testInternalProperties.put("TEST_DATA_PAYONE_KEY", getPayoneKey());
        testInternalProperties.put("TEST_DATA_PAYONE_MERCHANT_ID", getPayoneMerchantId());
        testInternalProperties.put("TEST_DATA_PAYONE_PORTAL_ID", getPayonePortalId());
        testInternalProperties.put("TEST_DATA_PAYONE_SUBACC_ID", getPayoneSubAccId());

        testInternalProperties.put("FIRST_TENANT_CT_PROJECT_KEY", getProjectKey());
        testInternalProperties.put("FIRST_TENANT_CT_CLIENT_ID", getClientId());
        testInternalProperties.put("FIRST_TENANT_CT_CLIENT_SECRET", getClientSecret());
        testInternalProperties.put("FIRST_TENANT_PAYONE_KEY", getPayoneKey());
        testInternalProperties.put("FIRST_TENANT_PAYONE_SUBACC_ID", getPayoneSubAccId());
        testInternalProperties.put("FIRST_TENANT_PAYONE_MERCHANT_ID", getPayoneMerchantId());
        testInternalProperties.put("FIRST_TENANT_PAYONE_PORTAL_ID", getPayonePortalId());

        PropertyProvider propertyProvider = Main.getPropertyProvider();

        List<Function<String, String>> propertiesGetters = propertyProvider.getPropertiesGetters();
        propertiesGetters.add(testInternalProperties::get);
        Main.main(new String[0]);
    }

    @Test
    public void verifyCreditCardTransferPayoneRequestIsSuccess() throws Exception {
        // Prepare
        final String paymentId = createPayment("CREDIT_CARD", "40", "EUR");

        // Test
        final HttpResponse httpResponse = executeGetRequest(format(URL_HANDLE_PAYMENT+paymentId, DEFAULT_PORT));

        // Assert
        assertThat(httpResponse.getStatusLine().getStatusCode()).isIn(HttpStatus.SC_ACCEPTED, HttpStatus.SC_OK);

        final PagedQueryResult<Payment> paymentPagedQueryResult =
            CTP_CLIENT.execute(PaymentQuery.of()
                                           .withPredicates(QueryPredicate.of(format("id=\"%s\"", paymentId))))
                      .toCompletableFuture().join();

        assertThat(paymentPagedQueryResult.getResults().get(0))
            .satisfies(
                payment -> {
                    assertThat(payment.getPaymentStatus().getInterfaceCode()).isEqualTo(APPROVED.toString());
                    assertThat(payment.getTransactions().get(0).getState()).isEqualTo(TransactionState.SUCCESS);
                });
    }

    @Test
    public void verifyPaypalWalletTransferPayoneAuthorizationRequestIsSuccess() throws Exception {
        // Prepare
        final String paymentId = preparePaymentWithAuthorizedAmountAndOrder();

        // Test
        final HttpResponse httpResponse = executeGetRequest(format(URL_HANDLE_PAYMENT+paymentId, DEFAULT_PORT));

        // Assert
        assertThat(httpResponse.getStatusLine().getStatusCode()).isIn(HttpStatus.SC_ACCEPTED, HttpStatus.SC_OK);

        final PagedQueryResult<Payment> paymentPagedQueryResult =
            CTP_CLIENT.execute(PaymentQuery.of()
                                           .withPredicates(QueryPredicate.of(format("id=\"%s\"", paymentId))))
                      .toCompletableFuture().join();

        assertThat(paymentPagedQueryResult.getResults().get(0))
            .satisfies(
                payment -> {
                    assertThat(payment.getPaymentStatus().getInterfaceCode()).isEqualTo(REDIRECT.toString());
                });
    }

    @Nonnull
    private static String preparePaymentWithAuthorizedAmountAndOrder() {
        final Map<String, Object> customFieldKeysMap = new HashMap<>();
        customFieldKeysMap.put(CustomFieldKeys.SUCCESS_URL_FIELD, "https://example.com/success");
        customFieldKeysMap.put(CustomFieldKeys.ERROR_URL_FIELD, "https://example.com/error");
        customFieldKeysMap.put(CustomFieldKeys.CANCEL_URL_FIELD, "https://example.com/cancel");
        customFieldKeysMap.put(CustomFieldKeys.REDIRECT_URL_FIELD, "https://example.com/redirect");
        customFieldKeysMap.put(CustomFieldKeys.REFERENCE_FIELD, String.valueOf(new Random().nextInt() + System.nanoTime()));
        customFieldKeysMap.put(CustomFieldKeys.LANGUAGE_CODE_FIELD, "en");

        final MonetaryAmount monetaryAmount = MoneyImpl.ofCents(4000, "EUR");
        final PaymentDraft paymentDraft =
            PaymentDraftBuilder.of(monetaryAmount)
                               .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                                                                          .method("WALLET-PAYPAL")
                                                                          .paymentInterface("PAYONE")
                                                                          .build())
                               .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                                   CustomTypeBuilder.PAYMENT_WALLET, customFieldKeysMap))
                               .build();

        final Payment payment = CTP_CLIENT.executeBlocking(PaymentCreateCommand.of(paymentDraft));
        final CartDraft cartDraft = CartDraftBuilder.of(Monetary.getCurrency("EUR")).build();

        CTP_CLIENT.executeBlocking(CartUpdateCommand.of(
            CTP_CLIENT.executeBlocking(CartCreateCommand.of(cartDraft)),
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
}
