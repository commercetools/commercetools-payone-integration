package com.commercetools.pspadapter.payone.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

/**
 * @author fhaertig
 * @date 15.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class PayoneConfigTest {

    private static final String dummyValue = "123";

    @Mock
    private PropertyProvider propertyProvider;

    @Before
    public void setUp() {
        when(propertyProvider.getProperty(anyString())).thenReturn(Optional.of(dummyValue));
        when(propertyProvider.getProperty(PropertyProvider.PAYONE_API_URL)).thenReturn(Optional.of("http://te.st"));
        when(propertyProvider.getMandatoryNonEmptyProperty(anyString())).thenReturn(dummyValue);
    }

    @Test
    public void getsSubAccountId() {
        when(propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_SUBACC_ID)).thenReturn("sub 1");
        assertThat(new PayoneConfig(propertyProvider).getSubAccountId()).isEqualTo("sub 1");
    }

    @Test
    public void throwsInCaseOfMissingSubAccountId() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(PropertyProvider.PAYONE_SUBACC_ID);
    }

    @Test
    public void getsMerchantId() {
        when(propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_MERCHANT_ID)).thenReturn("merchant");
        assertThat(new PayoneConfig(propertyProvider).getMerchantId()).isEqualTo("merchant");
    }

    @Test
    public void throwsInCaseOfMissingMerchantId() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(PropertyProvider.PAYONE_MERCHANT_ID);
    }

    @Test
    public void getsKeyAsMd5Hash() {
        when(propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_KEY)).thenReturn("key 1");
        assertThat(new PayoneConfig(propertyProvider).getKeyAsMd5Hash())
                .isEqualTo(Hashing.md5().hashString("key 1", Charsets.UTF_8).toString());
    }

    @Test
    public void throwsInCaseOfMissingKey() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(PropertyProvider.PAYONE_KEY);
    }

    @Test
    public void getsPortalId() {
        when(propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_PORTAL_ID)).thenReturn("portal 1");
        assertThat(new PayoneConfig(propertyProvider).getPortalId()).isEqualTo("portal 1");
    }

    @Test
    public void throwsInCaseOfMissingPortalId() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(PropertyProvider.PAYONE_PORTAL_ID);
    }

    @Test
    public void getsMode() {
        when(propertyProvider.getProperty(PropertyProvider.PAYONE_MODE)).thenReturn(Optional.of("mode x"));
        assertThat(new PayoneConfig(propertyProvider).getMode()).isEqualTo("mode x");
    }

    @Test
    public void defaultsToTestInCaseOfMissingMode() {
        when(propertyProvider.getProperty(PropertyProvider.PAYONE_MODE)).thenReturn(Optional.empty());
        assertThat(new PayoneConfig(propertyProvider).getMode()).isEqualTo(PayoneConfig.DEFAULT_PAYONE_MODE);
        assertThat(PayoneConfig.DEFAULT_PAYONE_MODE).isEqualTo("test");
    }

    @Test
    public void getsApiVersion() {
        when(propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_API_VERSION)).thenReturn("v1.0");
        assertThat(new PayoneConfig(propertyProvider).getApiVersion()).isEqualTo("v1.0");
    }

    @Test
    public void throwsInCaseOfMissingApiVersion() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(PropertyProvider.PAYONE_API_VERSION);
    }

    @Test
    public void getsPayoneApiUrl() {
        when(propertyProvider.getProperty(PropertyProvider.PAYONE_API_URL)).thenReturn(Optional.of("http://he.re/we/go"));
        assertThat(new PayoneConfig(propertyProvider).getApiUrl()).isEqualTo("http://he.re/we/go");
    }

    @Test
    public void defaultsToPayoneUrlInCaseOfMissingApiUrl() {
        when(propertyProvider.getProperty(PropertyProvider.PAYONE_API_URL)).thenReturn(Optional.empty());
        assertThat(new PayoneConfig(propertyProvider).getApiUrl()).isEqualTo(PayoneConfig.DEFAULT_PAYONE_API_URL);
    }

    private void assertThatThrowsInCaseOfMissingOrEmptyProperty(final String propertyName) {
        final IllegalStateException illegalStateException = new IllegalStateException();
        when(propertyProvider.getMandatoryNonEmptyProperty(propertyName)).thenThrow(illegalStateException);

        final Throwable throwable = catchThrowable(() -> new PayoneConfig(propertyProvider));

        assertThat(throwable).isSameAs(illegalStateException);
    }
}