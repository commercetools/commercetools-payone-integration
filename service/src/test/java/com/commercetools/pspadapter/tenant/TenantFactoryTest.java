package com.commercetools.pspadapter.tenant;

import com.commercetools.payments.TransactionStateResolver;
import com.commercetools.payments.TransactionStateResolverImpl;
import com.commercetools.pspadapter.payone.PaymentDispatcher;
import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.BaseRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaPreauthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.wallet.WalletAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.wallet.WalletPreauthorizationRequest;
import com.commercetools.pspadapter.payone.mapping.CountryToLanguageMapper;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentMethodInfoBuilder;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;

import javax.annotation.Nonnull;
import java.util.Map;

import static com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod.*;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.STATUS;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus.APPROVED;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.REFERENCE_FIELD;
import static io.sphere.sdk.payments.TransactionState.INITIAL;
import static io.sphere.sdk.payments.TransactionType.AUTHORIZATION;
import static io.sphere.sdk.payments.TransactionType.CHARGE;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TenantFactoryTest {

    @Mock
    private PayoneConfig payoneConfig;

    @Mock
    private TenantConfig tenantConfig;

    private TenantFactory factory;

    private final PaymentTestHelper testHelper = new PaymentTestHelper();

    private TransactionStateResolver transactionStateResolver = new TransactionStateResolverImpl();

    @Before
    public void setUp() throws Exception {
        when(payoneConfig.getApiUrl()).thenReturn("http://test.api.url");
        when(tenantConfig.getPayoneConfig()).thenReturn(payoneConfig);
        when(tenantConfig.getName()).thenReturn("testTenantName");
        when(tenantConfig.getSphereClientConfig())
                .thenReturn(SphereClientConfig.of("test-key", "test-client-id", "test-client-secret"));

        factory = new TenantFactory("testPayoneInterfaceName", tenantConfig);
    }

    @Test
    public void getsTenantName() throws Exception {
        assertThat(factory.getTenantName()).isEqualTo("testTenantName");
    }

    @Test
    public void getsPayoneInterfaceName() throws Exception {
        assertThat(factory.getPayoneInterfaceName()).isEqualTo("testPayoneInterfaceName");
    }

    @Test
    public void getsPaymentHandlerUrl() throws Exception {
        assertThat(factory.getPaymentHandlerUrl()).contains("/testTenantName/");
    }

    @Test
    public void getsPayoneNotificationUrl() throws Exception {
        assertThat(factory.getPayoneNotificationUrl()).contains("/testTenantName/");
    }

    @Test
    public void getsCustomTypeBuilderWithDeniedPermissions() throws Exception {
        CustomTypeBuilder customTypeBuilder = factory.getCustomTypeBuilder();
        assertThat(customTypeBuilder).isNotNull();
        assertThat(customTypeBuilder.getPermissionToStartFromScratch())
                .withFailMessage("It's important that the permission to start from scratch is DENIED by default")
                .isEqualTo(CustomTypeBuilder.PermissionToStartFromScratch.DENIED);
    }

    @Test
    public void getsCustomTypeBuilderWithGrantedPermission() throws Exception {
        when(tenantConfig.getStartFromScratch()).thenReturn(true);

        TenantFactory localFactory = new TenantFactory("testPayoneInterfaceName", tenantConfig);
        CustomTypeBuilder customTypeBuilder = localFactory.getCustomTypeBuilder();

        assertThat(customTypeBuilder).isNotNull();
        assertThat(customTypeBuilder.getPermissionToStartFromScratch())
                .withFailMessage("If tenantConfig.getStartFromScratch() is true - remove permission should be granted")
                .isEqualTo(CustomTypeBuilder.PermissionToStartFromScratch.GRANTED);
    }

    @Test
    public void createRequestFactory_klarna_preauthorization() throws Exception {
        PayoneRequestFactory requestFactory = factory.createRequestFactory(INVOICE_KLARNA, tenantConfig);
        PaymentWithCartLike paymentWithCartLike = testHelper.createKlarnaPaymentWithCartLike();
        AuthorizationRequest authorizationRequest = requestFactory.createPreauthorizationRequest(paymentWithCartLike);

        assertThat(authorizationRequest).isInstanceOf(KlarnaPreauthorizationRequest.class);
        Map<String, Object> actual = authorizationRequest.toStringMap(false);
        assertThat(actual.get("request")).isEqualTo("preauthorization");
        assertThat(actual.get("clearingtype")).isEqualTo("fnc");
        assertThat(actual.get("financingtype")).isEqualTo("KLV");

    }

    @Test
    public void createRequestFactory_klarna_authorization() throws Exception {
        PayoneRequestFactory requestFactory = factory.createRequestFactory(INVOICE_KLARNA, tenantConfig);
        PaymentWithCartLike paymentWithCartLike = testHelper.createKlarnaPaymentWithCartLike();
        AuthorizationRequest authorizationRequest = requestFactory.createAuthorizationRequest(paymentWithCartLike);

        assertThat(authorizationRequest).isInstanceOf(KlarnaAuthorizationRequest.class);
        Map<String, Object> actual = authorizationRequest.toStringMap(false);
        assertThat(actual.get("request")).isEqualTo("authorization");
        assertThat(actual.get("clearingtype")).isEqualTo("fnc");
        assertThat(actual.get("financingtype")).isEqualTo("KLV");
    }

    @Test
    public void createRequestFactory_paypal_preauthorization() throws Exception {
        PayoneRequestFactory requestFactory = factory.createRequestFactory(WALLET_PAYPAL, tenantConfig);
        PaymentWithCartLike paymentWithCartLike = testHelper.createPaypalPaymentWithCartLike();
        AuthorizationRequest authorizationRequest = requestFactory.createPreauthorizationRequest(paymentWithCartLike);

        assertThat(authorizationRequest).isInstanceOf(WalletPreauthorizationRequest.class);
        Map<String, Object> actual = authorizationRequest.toStringMap(false);
        assertThat(actual.get("request")).isEqualTo("preauthorization");
        assertThat(actual.get("clearingtype")).isEqualTo("wlt");
        assertThat(actual.get("wallettype")).isEqualTo("PPE");

    }

    @Test
    public void createRequestFactory_paypal_authorization() throws Exception {
        PayoneRequestFactory requestFactory = factory.createRequestFactory(WALLET_PAYPAL, tenantConfig);
        PaymentWithCartLike paymentWithCartLike = testHelper.createPaypalPaymentWithCartLike();
        AuthorizationRequest authorizationRequest = requestFactory.createAuthorizationRequest(paymentWithCartLike);

        assertThat(authorizationRequest).isInstanceOf(WalletAuthorizationRequest.class);
        Map<String, Object> actual = authorizationRequest.toStringMap(false);
        assertThat(actual.get("request")).isEqualTo("authorization");
        assertThat(actual.get("clearingtype")).isEqualTo("wlt");
        assertThat(actual.get("wallettype")).isEqualTo("PPE");
    }

    @Test
    public void createRequestFactory_paydirekt_preauthorization() throws Exception {
        PayoneRequestFactory requestFactory = factory.createRequestFactory(WALLET_PAYDIREKT, tenantConfig);
        PaymentWithCartLike paymentWithCartLike = testHelper.createPaydirektPaymentWithCartLike();
        AuthorizationRequest authorizationRequest = requestFactory.createPreauthorizationRequest(paymentWithCartLike);

        assertThat(authorizationRequest).isInstanceOf(WalletPreauthorizationRequest.class);
        Map<String, Object> actual = authorizationRequest.toStringMap(false);
        assertThat(actual.get("request")).isEqualTo("preauthorization");
        assertThat(actual.get("clearingtype")).isEqualTo("wlt");
        assertThat(actual.get("wallettype")).isEqualTo("PDT");

    }

    @Test
    public void createRequestFactory_paydirekt_authorization() throws Exception {
        PayoneRequestFactory requestFactory = factory.createRequestFactory(WALLET_PAYDIREKT, tenantConfig);
        PaymentWithCartLike paymentWithCartLike = testHelper.createPaydirektPaymentWithCartLike();
        AuthorizationRequest authorizationRequest = requestFactory.createAuthorizationRequest(paymentWithCartLike);

        assertThat(authorizationRequest).isInstanceOf(WalletAuthorizationRequest.class);
        Map<String, Object> actual = authorizationRequest.toStringMap(false);
        assertThat(actual.get("request")).isEqualTo("authorization");
        assertThat(actual.get("clearingtype")).isEqualTo("wlt");
        assertThat(actual.get("wallettype")).isEqualTo("PDT");
    }

    @Test
    public void createRequestFactory_bancontact_preauthorization() throws Exception {
        PayoneRequestFactory requestFactory = factory.createRequestFactory(BANK_TRANSFER_BANCONTACT, tenantConfig);
        PaymentWithCartLike paymentWithCartLike = testHelper.createBancontactPaymentWithCartLike();
        AuthorizationRequest authorizationRequest = requestFactory.createPreauthorizationRequest(paymentWithCartLike);

        assertThat(authorizationRequest).isInstanceOf(BankTransferRequest.class);
        Map<String, Object> actual = authorizationRequest.toStringMap(false);
        assertThat(actual.get("onlinebanktransfertype")).isEqualTo("BCT");
        assertThat(actual.get("request")).isEqualTo("preauthorization");
        assertThat(actual.get("clearingtype")).isEqualTo("sb");
    }

    @Test
    @SuppressWarnings("unchecked") // suppress mock(LoadingCache.class) generics warning
    public void createPaymentDispatcher() throws Exception {
        LoadingCache<String, Type> typeCache = mock(LoadingCache.class);
        BlockingSphereClient blockingSphereClient = mock(BlockingSphereClient.class);

        PaymentDispatcher paymentDispatcher = factory.createPaymentDispatcher(tenantConfig, typeCache, blockingSphereClient,
                mock(PayonePostService.class), transactionStateResolver);
        assertThat(paymentDispatcher).isNotNull();
    }

    /**
     * Verify all values from cartesian product of {@link PaymentMethod#supportedTransactionTypes} and
     * {@link PaymentMethod#supportedTransactionTypes} are processed by the service payment dispatcher,
     * e.g. proper request type is sent to Payone.
     */
    @Test
    public void createdMethodDispatcher_handlesAuthorizationAndCharge_forAllPaymentMethods() throws PayoneException {

        assertThat(supportedTransactionTypes).contains(AUTHORIZATION, CHARGE);
        assertThat(supportedPaymentMethods.size()).isGreaterThan(0); // at least one method is supported, isn't it?

        // 1. Iterate all supported payment methods
        for (PaymentMethod supportedPaymentMethod : supportedPaymentMethods) {
            // 2. Iterate all expected transaction types
            for (TransactionType supportedTransactionType : supportedTransactionTypes) {

                // 3. mock payment with required custom fields and transaction with specific AUTHORIZATION or CHARGE type
                Payment payment = mock(Payment.class);
                when(payment.getPaymentMethodInfo()).thenReturn(PaymentMethodInfoBuilder.of()
                        .paymentInterface("testPayoneInterfaceName")
                        .method(supportedPaymentMethod.getKey())
                        .build());

                CustomFields customFields = mock(CustomFields.class);
                when(payment.getCustom()).thenReturn(customFields);
                when(customFields.getFieldAsString(REFERENCE_FIELD)).thenReturn("test-createdMethodDispatcher");

                Transaction transaction = mock(Transaction.class);
                when(payment.getTransactions()).thenReturn(singletonList(transaction));
                when(transaction.getState()).thenReturn(INITIAL);

                when(transaction.getId()).thenReturn("test-transaction-uuid");
                when(transaction.getType()).thenReturn(supportedTransactionType); //AUTHORIZATION and CHARGE

                PaymentWithCartLike pwcl = testHelper.createDummyPaymentWithCartLike(payment, mock(Cart.class));

                // 4. mock sphere client and payone post service to return success
                BlockingSphereClient client = mock(BlockingSphereClient.class);
                when(client.executeBlocking(any(PaymentUpdateCommand.class))).thenReturn(payment);
                PayonePostService payonePostService = mock(PayonePostService.class);
                when(payonePostService.executePost(any(BaseRequest.class))).thenReturn(ImmutableMap.of(STATUS, APPROVED.getStateCode()));

                // 5. inject mocked CTP and Payone services to tenant factory
                TenantFactory factory = mockTenantFactory(tenantConfig, client, payonePostService);

                // 6. Make actual call. No exception should be thrown
                factory.getPaymentDispatcher().dispatchPayment(pwcl);

                // 7. Capture sent values and verify they have expected
                // "preauthorization" or "authorization" Payone request type
                ArgumentCaptor<BaseRequest> payoneRequestCaptor = ArgumentCaptor.forClass(BaseRequest.class);
                verify(payonePostService, times(1)).executePost(payoneRequestCaptor.capture());
                assertThat(payoneRequestCaptor.getValue()).isInstanceOf(AuthorizationRequest.class);
                AuthorizationRequest request = (AuthorizationRequest) payoneRequestCaptor.getValue();
                String expectedPayoneRequestType = mapCtpTransactionToPayoneRequestType(supportedTransactionType);
                String actualPayoneRequestType = request.getRequest();
                assertThat(actualPayoneRequestType)
                        .withFailMessage(format("Unexpected Payone request type for respective CTP transaction [%s] in payment type [%s].\n\tExpected: [%s]\n\tActual: [%s]",
                                supportedTransactionType.toSphereName(), supportedPaymentMethod.getKey(), expectedPayoneRequestType, actualPayoneRequestType))
                        .isEqualTo(expectedPayoneRequestType);
            }
        }
    }

    @Test
    public void createCountryToLanguageMapper() throws Exception {
        CountryToLanguageMapper paymentDispatcher = factory.createCountryToLanguageMapper();
        assertThat(paymentDispatcher).isNotNull();
    }

    @Test
    public void createBlockingSphereClient() throws Exception {
        SphereClient client = factory.createBlockingSphereClient(tenantConfig);
        assertThat(client).isNotNull();
        assertThat(BlockingSphereClient.class.isInstance(client)).isTrue();
    }

    /**
     * Because {@link TenantFactory} initializes {@link com.commercetools.pspadapter.payone.PaymentHandler}
     * and {@link PaymentDispatcher} inside constructor, we can't easily mock an instance, thus we make this anonymous
     * class extending.
     *
     * @param tenantConfig         {@link TenantConfig} to inject
     * @param blockingSphereClient {@link BlockingSphereClient} to inject
     * @param payonePostService    {@link PayonePostService} to inject
     * @return instance of {@link TenantFactory} with injected values from above.
     */
    private static TenantFactory mockTenantFactory(TenantConfig tenantConfig, BlockingSphereClient blockingSphereClient, PayonePostService payonePostService) {
        return new TenantFactory("testPayoneInterfaceName", tenantConfig) {
            @Nonnull
            @Override
            protected BlockingSphereClient createBlockingSphereClient(TenantConfig tenantConfig) {
                return blockingSphereClient;
            }

            @Nonnull
            @Override
            protected PayonePostService getPayonePostService(TenantConfig tenantConfig) {
                return payonePostService;
            }
        };
    }

    /**
     * <table border="1">
     * <tr><th>CTP</th><td>&nbsp;</td><th>Payone</th></tr>
     * <tr><td>{@link TransactionType#AUTHORIZATION}</td><td>&nbsp;-&gt;&nbsp;</td><td>{@link RequestType#PREAUTHORIZATION}</td></tr>
     * <tr><td>{@link TransactionType#CHARGE}</td><td>&nbsp;-&gt;&nbsp;</td><td>{@link RequestType#AUTHORIZATION}</td></tr>
     * <tr><td>other</td><td>&nbsp;-&gt;&nbsp;</td><td>"unexpected transaction request type "%s""</td></tr>
     * </table>
     *
     * @param ctpTransactionType CTP {@link TransactionType} to map to respective Payone request type
     * @return mapped string value like in the table above.
     */
    private static String mapCtpTransactionToPayoneRequestType(TransactionType ctpTransactionType) {
        return ctpTransactionType == AUTHORIZATION ? RequestType.PREAUTHORIZATION.getType() :
                (ctpTransactionType == CHARGE ? RequestType.AUTHORIZATION.getType() :
                        format("unexpected transaction request type \"%s\"", ctpTransactionType));
    }
}
