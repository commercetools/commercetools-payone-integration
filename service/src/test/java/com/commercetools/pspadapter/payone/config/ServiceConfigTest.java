package com.commercetools.pspadapter.payone.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static com.commercetools.pspadapter.payone.config.PropertyProvider.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * @author fhaertig
 * @since 03.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceConfigTest {

    private static final String dummyValue = "123";

    @Spy
    private PropertyProvider propertyProvider;

    @Test
    public void getsSingleTenantName() {
        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.of("testTenantName"));
        assertThat(new ServiceConfig(propertyProvider).getTenants()).containsOnly("testTenantName");
    }

    @Test
    public void getsApplicationInfo() {
        assertThat(propertyProvider.getMandatoryNonEmptyProperty(PAYONE_INTEGRATOR_NAME)).isEqualTo("DEBUG-TITLE");
        assertThat(propertyProvider.getMandatoryNonEmptyProperty(PAYONE_INTEGRATOR_VERSION)).isEqualTo("DEBUG-VERSION");

        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.of("testTenantName"));
        when(propertyProvider.getMandatoryNonEmptyProperty(PAYONE_INTEGRATOR_NAME)).thenReturn("testAppName");
        when(propertyProvider.getMandatoryNonEmptyProperty(PAYONE_INTEGRATOR_VERSION)).thenReturn("testAppVersion");

        ServiceConfig serviceConfig = new ServiceConfig(propertyProvider);
        assertThat(serviceConfig.getApplicationName()).isEqualTo("testAppName");
        assertThat(serviceConfig.getApplicationVersion()).isEqualTo("testAppVersion");
    }

    @Test
    public void getsMultipleTenantNameAndTrimsWhitespaces() {
        when(propertyProvider.getProperty(TENANTS))
                .thenReturn(Optional.of("\ttestTenantName1,   secondTestTenant ; andOneMoreTenant  , andTheLastOne "));
        assertThat(new ServiceConfig(propertyProvider).getTenants())
                .containsOnly("testTenantName1", "secondTestTenant", "andOneMoreTenant", "andTheLastOne");
    }

    @Test
    public void throwsExceptionIfTenantNameIsEmpty() throws Exception {
        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.empty());
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new ServiceConfig(propertyProvider).getTenants())
                .withMessageContaining(TENANTS);

        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.of(""));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new ServiceConfig(propertyProvider).getTenants())
                .withMessageContaining(TENANTS);

        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.of("   "));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new ServiceConfig(propertyProvider).getTenants())
                .withMessageContaining(TENANTS);

        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.of(" ,  "));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new ServiceConfig(propertyProvider).getTenants())
                .withMessageContaining(TENANTS);

        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.of("one,,two"));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new ServiceConfig(propertyProvider).getTenants())
                .withMessageContaining(TENANTS);

        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.of(" one ,  , two"));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new ServiceConfig(propertyProvider).getTenants())
                .withMessageContaining(TENANTS);
    }

    private static void mockDummyDefault(PropertyProvider propertyProvider) {
        doReturn(Optional.of(dummyValue)).when(propertyProvider).getProperty(anyString());
    }
}