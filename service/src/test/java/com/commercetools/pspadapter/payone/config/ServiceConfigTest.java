package com.commercetools.pspadapter.payone.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

/**
 * @author fhaertig
 * @date 03.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceConfigTest {

    private static final String dummyValue = "123";

    @Mock
    private PropertyProvider propertyProvider;

    @Test
    public void getCtProjectKey() {
        when(propertyProvider.getEnvironmentOrSystemValue(any())).thenReturn(Optional.of(dummyValue));
        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.CT_PROJECT_KEY)).thenReturn(Optional.empty());
        when(propertyProvider.createIllegalArgumentException(any())).thenCallRealMethod();

        final Throwable throwable = catchThrowable(() -> new ServiceConfig(propertyProvider));

        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value of " + PropertyProvider.CT_PROJECT_KEY + " is required and can not be empty!");

        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.CT_PROJECT_KEY)).thenReturn(Optional.of(dummyValue));

        assertThat(new ServiceConfig(propertyProvider).getCtProjectKey()).isEqualTo(dummyValue);
    }

    @Test
    public void getCtClientId() {
        when(propertyProvider.getEnvironmentOrSystemValue(any())).thenReturn(Optional.of(dummyValue));
        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.CT_CLIENT_ID)).thenReturn(Optional.empty());
        when(propertyProvider.createIllegalArgumentException(any())).thenCallRealMethod();

        final Throwable throwable = catchThrowable(() -> new ServiceConfig(propertyProvider));

        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value of " + PropertyProvider.CT_CLIENT_ID + " is required and can not be empty!");

        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.CT_CLIENT_ID)).thenReturn(Optional.of(dummyValue));

        assertThat(new ServiceConfig(propertyProvider).getCtClientId()).isEqualTo(dummyValue);
    }

    @Test
    public void getCtClientSecret() {
        when(propertyProvider.getEnvironmentOrSystemValue(any())).thenReturn(Optional.of(dummyValue));
        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.CT_CLIENT_SECRET)).thenReturn(Optional.empty());
        when(propertyProvider.createIllegalArgumentException(any())).thenCallRealMethod();

        final Throwable throwable = catchThrowable(() -> new ServiceConfig(propertyProvider));

        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value of " + PropertyProvider.CT_CLIENT_SECRET + " is required and can not be empty!");

        when(propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.CT_CLIENT_SECRET)).thenReturn(Optional.of(dummyValue));

        assertThat(new ServiceConfig(propertyProvider).getCtClientSecret()).isEqualTo(dummyValue);
    }
}
