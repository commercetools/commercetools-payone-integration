package com.commercetools.pspadapter.payone.config;

import com.commercetools.pspadapter.payone.util.PayoneHash;
import com.commercetools.pspadapter.tenant.TenantPropertyProvider;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author fhaertig
 * @since 15.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class PayoneConfigTest {

    private static final IllegalStateException illegalStateException = new IllegalStateException();

    private static final String commonDummyValue = "123";

    private static final String tenantDummyValue = "456";

    @Mock
    private TenantPropertyProvider tenantPropertyProvider;

    @Mock
    private PropertyProvider propertyProvider;

    @Before
    public void setUp() {
        when(propertyProvider.getProperty(anyString())).thenReturn(Optional.of(commonDummyValue));
        when(propertyProvider.getProperty(PropertyProvider.PAYONE_API_URL)).thenReturn(Optional.of("http://te.st"));
        when(propertyProvider.getMandatoryNonEmptyProperty(anyString())).thenReturn(commonDummyValue);

        when(tenantPropertyProvider.getCommonPropertyProvider()).thenReturn(propertyProvider);
        when(tenantPropertyProvider.getTenantProperty(anyString())).thenReturn(Optional.of(tenantDummyValue));
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(anyString())).thenReturn(tenantDummyValue);
    }

    @Test
    public void getsSubAccountId() {
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(TenantPropertyProvider.PAYONE_SUBACC_ID)).thenReturn("sub 1");
        assertThat(new PayoneConfig(tenantPropertyProvider).getSubAccountId()).isEqualTo("sub 1");
    }

    @Test
    public void throwsInCaseOfMissingSubAccountId() {
        assertThatThrowsInCaseOfMissingOrEmptyTenantProperty(TenantPropertyProvider.PAYONE_SUBACC_ID);
    }

    @Test
    public void getsMerchantId() {
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(TenantPropertyProvider.PAYONE_MERCHANT_ID)).thenReturn("merchant");
        assertThat(new PayoneConfig(tenantPropertyProvider).getMerchantId()).isEqualTo("merchant");
    }

    @Test
    public void throwsInCaseOfMissingMerchantId() {
        assertThatThrowsInCaseOfMissingOrEmptyTenantProperty(TenantPropertyProvider.PAYONE_MERCHANT_ID);
    }

    @Test
    public void getsKeyAsHash() {
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(TenantPropertyProvider.PAYONE_KEY)).thenReturn("key 1");
        assertThat(new PayoneConfig(tenantPropertyProvider).getKeyAsHash())
                .isEqualTo(PayoneHash.calculate("key 1"));
    }

    @Test
    public void throwsInCaseOfMissingKey() {
        assertThatThrowsInCaseOfMissingOrEmptyTenantProperty(TenantPropertyProvider.PAYONE_KEY);
    }

    @Test
    public void getsPortalId() {
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(TenantPropertyProvider.PAYONE_PORTAL_ID)).thenReturn("portal 1");
        assertThat(new PayoneConfig(tenantPropertyProvider).getPortalId()).isEqualTo("portal 1");
    }

    @Test
    public void throwsInCaseOfMissingPortalId() {
        assertThatThrowsInCaseOfMissingOrEmptyTenantProperty(TenantPropertyProvider.PAYONE_PORTAL_ID);
    }

    @Test
    public void getsMode() {
        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.PAYONE_MODE)).thenReturn(Optional.of("mode x"));
        assertThat(new PayoneConfig(tenantPropertyProvider).getMode()).isEqualTo("mode x");
    }

    @Test
    public void defaultsToTestInCaseOfMissingMode() {
        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.PAYONE_MODE)).thenReturn(Optional.empty());
        assertThat(new PayoneConfig(tenantPropertyProvider).getMode()).isEqualTo(PayoneConfig.DEFAULT_PAYONE_MODE);
        assertThat(PayoneConfig.DEFAULT_PAYONE_MODE).isEqualTo("test");
    }

    @Test
    public void getsApiVersion() {
        when(propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_API_VERSION)).thenReturn("v1.0");
        assertThat(new PayoneConfig(tenantPropertyProvider).getApiVersion()).isEqualTo("v1.0");
    }

    @Test
    public void throwsInCaseOfMissingApiVersion() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(PropertyProvider.PAYONE_API_VERSION);
    }

    @Test
    public void getsEncoding() {
        when(propertyProvider.getProperty(PropertyProvider.PAYONE_REQUEST_ENCODING)).thenReturn(Optional.of("ISO 8859-1"));
        assertThat(new PayoneConfig(tenantPropertyProvider).getEncoding()).isEqualTo("ISO 8859-1");
    }

    @Test
    public void defaultsToTestInCaseOfMissingEncoding() {
        when(propertyProvider.getProperty(PropertyProvider.PAYONE_REQUEST_ENCODING)).thenReturn(Optional.empty());
        assertThat(new PayoneConfig(tenantPropertyProvider).getEncoding()).isEqualTo(PayoneConfig.DEFAULT_PAYONE_REQUEST_ENCODING);
        assertThat(PayoneConfig.DEFAULT_PAYONE_REQUEST_ENCODING).isEqualTo("UTF-8");
    }

    @Test
    public void getsPayoneApiUrl() {
        when(propertyProvider.getProperty(PropertyProvider.PAYONE_API_URL)).thenReturn(Optional.of("http://he.re/we/go"));
        assertThat(new PayoneConfig(tenantPropertyProvider).getApiUrl()).isEqualTo("http://he.re/we/go");
    }

    @Test
    public void defaultsToPayoneUrlInCaseOfMissingApiUrl() {
        when(propertyProvider.getProperty(PropertyProvider.PAYONE_API_URL)).thenReturn(Optional.empty());
        assertThat(new PayoneConfig(tenantPropertyProvider).getApiUrl()).isEqualTo(PayoneConfig.DEFAULT_PAYONE_API_URL);
    }

    private void assertThatThrowsInCaseOfMissingOrEmptyProperty(final String propertyName) {
        when(propertyProvider.getMandatoryNonEmptyProperty(propertyName)).thenThrow(illegalStateException);
        assertThatThrows();
    }

    private void assertThatThrowsInCaseOfMissingOrEmptyTenantProperty(final String propertyName) {
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(propertyName)).thenThrow(illegalStateException);
        assertThatThrows();
    }

    private void assertThatThrows() {
        final Throwable throwable = catchThrowable(() -> new PayoneConfig(tenantPropertyProvider));
        assertThat(throwable).isSameAs(illegalStateException);
    }
}