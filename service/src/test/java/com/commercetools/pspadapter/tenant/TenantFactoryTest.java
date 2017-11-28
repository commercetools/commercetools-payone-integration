package com.commercetools.pspadapter.tenant;

import com.commercetools.payments.TransactionStateResolver;
import com.commercetools.payments.TransactionStateResolverImpl;
import com.commercetools.pspadapter.payone.PaymentDispatcher;
import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaPreauthorizationRequest;
import com.commercetools.pspadapter.payone.mapping.CountryToLanguageMapper;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.google.common.cache.LoadingCache;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.types.Type;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;

import java.util.Map;

import static com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod.INVOICE_KLARNA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    @SuppressWarnings("unchecked") // suppress mock(LoadingCache.class) generics warning
    public void createPaymentDispatcher() throws Exception {
        LoadingCache<String, Type> typeCache = mock(LoadingCache.class);
        BlockingSphereClient blockingSphereClient = mock(BlockingSphereClient.class);

        PaymentDispatcher paymentDispatcher = factory.createPaymentDispatcher(tenantConfig, typeCache,
                blockingSphereClient, transactionStateResolver);
        assertThat(paymentDispatcher).isNotNull();
    }

    @Test
    public void createCountryToLanguageMapper() throws Exception {
        CountryToLanguageMapper paymentDispatcher = factory.createCountryToLanguageMapper();
        assertThat(paymentDispatcher).isNotNull();
    }
}