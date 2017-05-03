package com.commercetools.pspadapter.tenant;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import io.sphere.sdk.client.SphereClientConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static com.commercetools.pspadapter.tenant.TenantPropertyProvider.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantConfigTest {

    private static final String tenantDummyValue = "tyuiieo";

    @Mock
    private TenantPropertyProvider tenantPropertyProvider;

    @Mock
    private PayoneConfig payoneConfig;

    @Mock
    private PropertyProvider propertyProvider;

    @Before
    public void setUp() throws Exception {
        when(tenantPropertyProvider.getCommonPropertyProvider()).thenReturn(propertyProvider);
        when(tenantPropertyProvider.getTenantProperty(anyString())).thenReturn(Optional.of(tenantDummyValue));
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(anyString())).thenReturn(tenantDummyValue);
    }

    @Test
    public void getsName() throws Exception {
        when(tenantPropertyProvider.getTenantName()).thenReturn("xdr");
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).getName()).isEqualTo("xdr");
    }

    @Test
    public void getsPayoneConfig() throws Exception {
        when(payoneConfig.getMerchantId()).thenReturn("mockMID");
        when(payoneConfig.getPortalId()).thenReturn("mockPID");
        when(payoneConfig.getApiUrl()).thenReturn("mockApiUrl");
        when(payoneConfig.getEncoding()).thenReturn("mockEnc");

        final PayoneConfig toTest = new TenantConfig(tenantPropertyProvider, payoneConfig).getPayoneConfig();
        assertThat(toTest.getMerchantId()).isEqualTo("mockMID");
        assertThat(toTest.getPortalId()).isEqualTo("mockPID");
        assertThat(toTest.getApiUrl()).isEqualTo("mockApiUrl");
        assertThat(toTest.getEncoding()).isEqualTo("mockEnc");
    }

    @Test
    public void getsSphereClientConfig() throws Exception {
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(CT_PROJECT_KEY)).thenReturn("testCtKey");
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(CT_CLIENT_ID)).thenReturn("testCtId");
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(CT_CLIENT_SECRET)).thenReturn("testCtSecret");

        final SphereClientConfig sphereClientConfig = new TenantConfig(tenantPropertyProvider, payoneConfig).getSphereClientConfig();

        assertThat(sphereClientConfig.getProjectKey()).isEqualTo("testCtKey");
        assertThat(sphereClientConfig.getClientId()).isEqualTo("testCtId");
        assertThat(sphereClientConfig.getClientSecret()).isEqualTo("testCtSecret");
    }

    @Test
    public void getsCtProjectKey() {
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(TenantPropertyProvider.CT_PROJECT_KEY)).thenReturn("project X");
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).getSphereClientConfig().getProjectKey()).isEqualTo("project X");

        assertThatThrowsInCaseOfMissingOrEmptyProperty(TenantPropertyProvider.CT_PROJECT_KEY);
    }

    @Test
    public void getsCtClientId() {
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(TenantPropertyProvider.CT_CLIENT_ID)).thenReturn("id X");
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).getSphereClientConfig().getClientId()).isEqualTo("id X");

        assertThatThrowsInCaseOfMissingOrEmptyProperty(TenantPropertyProvider.CT_CLIENT_ID);
    }


    @Test
    public void getsCtClientSecret() {
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(TenantPropertyProvider.CT_CLIENT_SECRET)).thenReturn("secret X");
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).getSphereClientConfig().getClientSecret()).isEqualTo("secret X");

        assertThatThrowsInCaseOfMissingOrEmptyProperty(TenantPropertyProvider.CT_CLIENT_SECRET);
    }

    @Test
    public void getsStartFromScratch() throws Exception {
        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.CT_START_FROM_SCRATCH)).thenReturn(Optional.of("true"));
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).getStartFromScratch()).isEqualTo(true);

        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.CT_START_FROM_SCRATCH)).thenReturn(Optional.of("TRUE"));
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).getStartFromScratch()).isEqualTo(true);

        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.CT_START_FROM_SCRATCH)).thenReturn(Optional.of("false"));
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).getStartFromScratch()).isEqualTo(false);

        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.CT_START_FROM_SCRATCH)).thenReturn(Optional.of("FALSE"));
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).getStartFromScratch()).isEqualTo(false);

        // It's important that this property is FALSE by default!!!
        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.CT_START_FROM_SCRATCH)).thenReturn(Optional.of(""));
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).getStartFromScratch()).isEqualTo(false);

        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.CT_START_FROM_SCRATCH)).thenReturn(Optional.of("some-unexpected-value"));
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).getStartFromScratch()).isEqualTo(false);

        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.CT_START_FROM_SCRATCH)).thenReturn(Optional.empty());
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).getStartFromScratch()).isEqualTo(false);
    }

    @Test
    public void getsIsUpdateOrderPaymentState() {
        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.UPDATE_ORDER_PAYMENT_STATE)).thenReturn(Optional.of("true"));
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).isUpdateOrderPaymentState()).isEqualTo(true);

        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.UPDATE_ORDER_PAYMENT_STATE)).thenReturn(Optional.of("TRUE"));
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).isUpdateOrderPaymentState()).isEqualTo(true);

        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.UPDATE_ORDER_PAYMENT_STATE)).thenReturn(Optional.of("false"));
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).isUpdateOrderPaymentState()).isEqualTo(false);

        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.UPDATE_ORDER_PAYMENT_STATE)).thenReturn(Optional.of("FALSE"));
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).isUpdateOrderPaymentState()).isEqualTo(false);

        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.UPDATE_ORDER_PAYMENT_STATE)).thenReturn(Optional.of(""));
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).isUpdateOrderPaymentState()).isEqualTo(false);

        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.UPDATE_ORDER_PAYMENT_STATE)).thenReturn(Optional.empty());
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).isUpdateOrderPaymentState()).isEqualTo(false);
    }

    @Test
    public void getsSecureKey() throws Exception {
        when(tenantPropertyProvider.getTenantProperty(SECURE_KEY)).thenReturn(Optional.of("key-key-key"));
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).getSecureKey()).isEqualTo("key-key-key");

        when(tenantPropertyProvider.getTenantProperty(SECURE_KEY)).thenReturn(Optional.empty());
        assertThat(new TenantConfig(tenantPropertyProvider, payoneConfig).getSecureKey()).isEqualTo("");
    }

    private void assertThatThrowsInCaseOfMissingOrEmptyProperty(final String propertyName) {
        final IllegalStateException illegalStateException = new IllegalStateException();
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(propertyName)).thenThrow(illegalStateException);

        final Throwable throwable = catchThrowable(() -> new TenantConfig(tenantPropertyProvider, payoneConfig));

        assertThat(throwable).isSameAs(illegalStateException);
    }

}