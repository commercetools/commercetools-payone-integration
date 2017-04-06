package com.commercetools.pspadapter.payone.config;

import com.commercetools.pspadapter.tenant.TenantPropertyProvider;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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

    private static final String dummyValue = "123";

    @Mock
    private TenantPropertyProvider tenantPropertyProvider;

    @Before
    public void setUp() {
        when(tenantPropertyProvider.getTenantProperty(anyString())).thenReturn(Optional.of(dummyValue));
        when(tenantPropertyProvider.getTenantProperty(PropertyProvider.PAYONE_API_URL)).thenReturn(Optional.of("http://te.st"));
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(anyString())).thenReturn(dummyValue);
    }

    @Test
    public void getsSubAccountId() {
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(TenantPropertyProvider.PAYONE_SUBACC_ID)).thenReturn("sub 1");
        assertThat(new PayoneConfig(tenantPropertyProvider).getSubAccountId()).isEqualTo("sub 1");
    }

    @Test
    public void throwsInCaseOfMissingSubAccountId() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(TenantPropertyProvider.PAYONE_SUBACC_ID);
    }

    @Test
    public void getsMerchantId() {
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(TenantPropertyProvider.PAYONE_MERCHANT_ID)).thenReturn("merchant");
        assertThat(new PayoneConfig(tenantPropertyProvider).getMerchantId()).isEqualTo("merchant");
    }

    @Test
    public void throwsInCaseOfMissingMerchantId() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(TenantPropertyProvider.PAYONE_MERCHANT_ID);
    }

    @Test
    public void getsKeyAsMd5Hash() {
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(TenantPropertyProvider.PAYONE_KEY)).thenReturn("key 1");
        assertThat(new PayoneConfig(tenantPropertyProvider).getKeyAsMd5Hash())
                .isEqualTo(Hashing.md5().hashString("key 1", Charsets.UTF_8).toString());
    }

    @Test
    public void throwsInCaseOfMissingKey() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(TenantPropertyProvider.PAYONE_KEY);
    }

    @Test
    public void getsPortalId() {
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(TenantPropertyProvider.PAYONE_PORTAL_ID)).thenReturn("portal 1");
        assertThat(new PayoneConfig(tenantPropertyProvider).getPortalId()).isEqualTo("portal 1");
    }

    @Test
    public void throwsInCaseOfMissingPortalId() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(TenantPropertyProvider.PAYONE_PORTAL_ID);
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
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(PropertyProvider.PAYONE_API_VERSION)).thenReturn("v1.0");
        assertThat(new PayoneConfig(tenantPropertyProvider).getApiVersion()).isEqualTo("v1.0");
    }

    @Test
    public void throwsInCaseOfMissingApiVersion() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(PropertyProvider.PAYONE_API_VERSION);
    }

    @Test
    public void getsEncoding() {
        when(tenantPropertyProvider.getTenantProperty(PropertyProvider.PAYONE_REQUEST_ENCODING)).thenReturn(Optional.of("ISO 8859-1"));
        assertThat(new PayoneConfig(tenantPropertyProvider).getEncoding()).isEqualTo("ISO 8859-1");
    }

    @Test
    public void defaultsToTestInCaseOfMissingEncoding() {
        when(tenantPropertyProvider.getTenantProperty(PropertyProvider.PAYONE_REQUEST_ENCODING)).thenReturn(Optional.empty());
        assertThat(new PayoneConfig(tenantPropertyProvider).getEncoding()).isEqualTo(PayoneConfig.DEFAULT_PAYONE_REQUEST_ENCODING);
        assertThat(PayoneConfig.DEFAULT_PAYONE_REQUEST_ENCODING).isEqualTo("UTF-8");
    }

    @Test
    public void getsPayoneApiUrl() {
        when(tenantPropertyProvider.getTenantProperty(PropertyProvider.PAYONE_API_URL)).thenReturn(Optional.of("http://he.re/we/go"));
        assertThat(new PayoneConfig(tenantPropertyProvider).getApiUrl()).isEqualTo("http://he.re/we/go");
    }

    @Test
    public void defaultsToPayoneUrlInCaseOfMissingApiUrl() {
        when(tenantPropertyProvider.getTenantProperty(PropertyProvider.PAYONE_API_URL)).thenReturn(Optional.empty());
        assertThat(new PayoneConfig(tenantPropertyProvider).getApiUrl()).isEqualTo(PayoneConfig.DEFAULT_PAYONE_API_URL);
    }

    private void assertThatThrowsInCaseOfMissingOrEmptyProperty(final String propertyName) {
        final IllegalStateException illegalStateException = new IllegalStateException();
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(propertyName)).thenThrow(illegalStateException);

        final Throwable throwable = catchThrowable(() -> new PayoneConfig(tenantPropertyProvider));

        assertThat(throwable).isSameAs(illegalStateException);
    }
}