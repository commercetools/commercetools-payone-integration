package com.commercetools.pspadapter.payone.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;
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

    @Test
    public void getSubAccountId() {
        when(propertyProvider.getEnvironmentOrSystemValue(any())).thenReturn(Optional.of(dummyValue));
        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.PAYONE_SUBACC_ID)).thenReturn(Optional.empty());
        when(propertyProvider.createIllegalArgumentException(any())).thenCallRealMethod();

        final Throwable throwable = catchThrowable(() -> new PayoneConfig(propertyProvider));

        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value of " + PropertyProvider.PAYONE_SUBACC_ID + " is required and can not be empty!");

        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.PAYONE_SUBACC_ID)).thenReturn(Optional.of(dummyValue));

        assertThat(new PayoneConfig(propertyProvider).getSubAccountId()).isEqualTo(dummyValue);
    }

    @Test
    public void getMerchantId() {
        when(propertyProvider.getEnvironmentOrSystemValue(any())).thenReturn(Optional.of(dummyValue));
        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.PAYONE_MERCHANT_ID)).thenReturn(Optional.empty());
        when(propertyProvider.createIllegalArgumentException(any())).thenCallRealMethod();

        final Throwable throwable = catchThrowable(() -> new PayoneConfig(propertyProvider));

        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value of " + PropertyProvider.PAYONE_MERCHANT_ID + " is required and can not be empty!");

        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.PAYONE_MERCHANT_ID)).thenReturn(Optional.of(dummyValue));

        assertThat(new PayoneConfig(propertyProvider).getMerchantId()).isEqualTo(dummyValue);
    }

    @Test
    public void getKeyAsMd5Hash() {
        when(propertyProvider.getEnvironmentOrSystemValue(any())).thenReturn(Optional.of(dummyValue));
        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.PAYONE_KEY)).thenReturn(Optional.empty());
        when(propertyProvider.createIllegalArgumentException(any())).thenCallRealMethod();

        final Throwable throwable = catchThrowable(() -> new PayoneConfig(propertyProvider));

        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value of " + PropertyProvider.PAYONE_KEY + " is required and can not be empty!");

        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.PAYONE_KEY)).thenReturn(Optional.of(dummyValue));

        assertThat(new PayoneConfig(propertyProvider).getKeyAsMd5Hash())
                .isEqualTo(Hashing.md5().hashString(dummyValue, Charsets.UTF_8).toString());
    }

    @Test
    public void getPortalId() {
        when(propertyProvider.getEnvironmentOrSystemValue(any())).thenReturn(Optional.of(dummyValue));
        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.PAYONE_PORTAL_ID)).thenReturn(Optional.empty());
        when(propertyProvider.createIllegalArgumentException(any())).thenCallRealMethod();

        final Throwable throwable = catchThrowable(() -> new PayoneConfig(propertyProvider));

        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value of " + PropertyProvider.PAYONE_PORTAL_ID + " is required and can not be empty!");

        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.PAYONE_PORTAL_ID)).thenReturn(Optional.of(dummyValue));

        assertThat(new PayoneConfig(propertyProvider).getPortalId()).isEqualTo(dummyValue);
    }

    @Test
    public void getMode() {
        when(propertyProvider.getEnvironmentOrSystemValue(any())).thenReturn(Optional.of(dummyValue));
        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.PAYONE_MODE)).thenReturn(Optional.empty());

        assertThat(new PayoneConfig(propertyProvider).getMode()).isEqualTo(PayoneConfig.DEFAULT_PAYONE_MODE);

        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.PAYONE_MODE)).thenReturn(Optional.of(dummyValue));

        assertThat(new PayoneConfig(propertyProvider).getMode()).isEqualTo(dummyValue);
    }

    @Test
    public void getApiVersion() {
        Map<String, String> internalProperties = ImmutableMap.<String, String>builder()
                .put(PropertyProvider.PAYONE_API_VERSION, "3.9")
                .build();

        when(propertyProvider.getEnvironmentOrSystemValue(any())).thenReturn(Optional.of(dummyValue));
        when(propertyProvider.getInternalProperties()).thenReturn(internalProperties);
        when(propertyProvider.createIllegalArgumentException(any())).thenCallRealMethod();

        assertThat(new PayoneConfig(propertyProvider).getApiVersion()).isEqualTo("3.9");
    }

    @Test
    public void getPayoneApiUrl() {
        when(propertyProvider.getEnvironmentOrSystemValue(any())).thenReturn(Optional.of(dummyValue));
        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.PAYONE_API_URL)).thenReturn(Optional.empty());

        assertThat(new PayoneConfig(propertyProvider).getApiUrl()).isEqualTo(PayoneConfig.DEFAULT_PAYONE_API_URL);

        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.PAYONE_API_URL)).thenReturn(Optional.of(dummyValue));

        assertThat(new PayoneConfig(propertyProvider).getApiUrl()).isEqualTo(dummyValue);
    }
}