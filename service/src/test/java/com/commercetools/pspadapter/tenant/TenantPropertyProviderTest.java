package com.commercetools.pspadapter.tenant;

import com.commercetools.pspadapter.payone.config.PropertyProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantPropertyProviderTest {

    @Spy
    private PropertyProvider propertyProvider = new PropertyProvider();

    private TenantPropertyProvider tenantPropertyProvider;

    @Before
    public void setUp() throws Exception {
        tenantPropertyProvider = new TenantPropertyProvider("testTenantName", propertyProvider);
    }

    @Test
    public void getTenantName() throws Exception {
        assertThat(tenantPropertyProvider.getTenantName()).isEqualTo("testTenantName");
    }

    @Test
    public void getCommonPropertyProvider() throws Exception {
        assertThat(tenantPropertyProvider.getCommonPropertyProvider()).isSameAs(propertyProvider);
    }

    @Test
    public void getTenantProperty() throws Exception {
        when(propertyProvider.getProperty("XXX")).thenReturn(Optional.of("foo"));
        when(propertyProvider.getProperty("YYY")).thenReturn(Optional.of("bar"));
        when(propertyProvider.getProperty("testTenantName_XXX")).thenReturn(Optional.of("hallo"));
        when(propertyProvider.getProperty("testTenantName_YYY")).thenReturn(Optional.of("woot"));

        assertThat(tenantPropertyProvider.getCommonPropertyProvider().getProperty("XXX").orElse(null))
                .isEqualTo("foo");
        assertThat(tenantPropertyProvider.getTenantProperty("XXX").orElse(null))
                .isEqualTo("hallo");

        assertThat(tenantPropertyProvider.getCommonPropertyProvider().getProperty("YYY").orElse(null))
                .isEqualTo("bar");
        assertThat(tenantPropertyProvider.getTenantProperty("YYY").orElse(null))
                .isEqualTo("woot");

        assertThat(tenantPropertyProvider.getTenantProperty("ZZZ")).isEqualTo(Optional.empty());
    }

    @Test
    public void getTenantMandatoryNonEmptyProperty() throws Exception {
        when(propertyProvider.getProperty("XXX")).thenReturn(Optional.of("foo"));
        when(propertyProvider.getProperty("YYY")).thenReturn(Optional.of("bar"));
        when(propertyProvider.getProperty("testTenantName_XXX")).thenReturn(Optional.of("hallo"));
        when(propertyProvider.getProperty("testTenantName_YYY")).thenReturn(Optional.of("woot"));

        when(propertyProvider.getProperty("ZZZ")).thenReturn(Optional.of("foo-bar"));

        assertThat(tenantPropertyProvider.getCommonPropertyProvider().getMandatoryNonEmptyProperty("XXX"))
                .isEqualTo("foo");
        assertThat(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty("XXX"))
                .isEqualTo("hallo");

        assertThat(tenantPropertyProvider.getCommonPropertyProvider().getMandatoryNonEmptyProperty("YYY"))
                .isEqualTo("bar");
        assertThat(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty("YYY"))
                .isEqualTo("woot");

        // ZZZ is set for common, but not for tenant specific
        assertThat(tenantPropertyProvider.getCommonPropertyProvider().getMandatoryNonEmptyProperty("ZZZ"))
                .withFailMessage("Common property provider expected to have ZZZ property equal to \"foo-bar\"")
                .isEqualTo("foo-bar");
        assertThatThrownBy(() -> tenantPropertyProvider.getTenantMandatoryNonEmptyProperty("ZZZ"))
                .withFailMessage("Tenant property provider should not have property ZZZ")
                .isInstanceOf(IllegalStateException.class);
    }

}