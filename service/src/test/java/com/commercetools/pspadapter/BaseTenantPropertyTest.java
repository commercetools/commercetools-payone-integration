package com.commercetools.pspadapter;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.pspadapter.tenant.TenantPropertyProvider;
import org.junit.Before;
import org.mockito.Mock;
import util.PaymentTestHelper;

import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class BaseTenantPropertyTest {

    protected final static PaymentTestHelper paymentsTestHelper = new PaymentTestHelper();

    protected static final String dummyPropertyValue = "123";
    protected static final String dummyTenantValue = "456";

    @Mock
    protected TenantPropertyProvider tenantPropertyProvider;

    @Mock
    protected PropertyProvider propertyProvider;

    @Mock
    protected TenantConfig tenantConfig;

    protected PayoneConfig payoneConfig;

    @Before
    public void setUp() throws Exception {
        // 1) inject dummy values to tenant and common properties providers
        // 2) inject propertyProvider to tenantPropertyProvider
        // 3) inject payoneConfig to tenant config
        when(propertyProvider.getProperty(any())).thenReturn(Optional.of(dummyPropertyValue));
        when(propertyProvider.getMandatoryNonEmptyProperty(any())).thenReturn(dummyPropertyValue);

        when(tenantPropertyProvider.getCommonPropertyProvider()).thenReturn(propertyProvider);
        when(tenantPropertyProvider.getTenantProperty(anyString())).thenReturn(Optional.of(dummyTenantValue));
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(anyString())).thenReturn(dummyTenantValue);

        payoneConfig = new PayoneConfig(tenantPropertyProvider);

        when(tenantConfig.getPayoneConfig()).thenReturn(payoneConfig);
    }
}
