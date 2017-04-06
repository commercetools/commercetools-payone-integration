package com.commercetools.pspadapter.tenant;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantFactoryTest {

    @Mock
    private PayoneConfig payoneConfig;

    @Mock
    private TenantConfig tenantConfig;

    private TenantFactory factory;

    @Before
    public void setUp() throws Exception {
        when(payoneConfig.getApiUrl()).thenReturn("http://test.api.url");
        when(tenantConfig.getPayoneConfig()).thenReturn(payoneConfig);
        when(tenantConfig.getName()).thenReturn("testTenantName");

        factory = new TenantFactory("testPayoneInterfaceName", tenantConfig);
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

}