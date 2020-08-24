package com.commercetools.util;

import com.commercetools.pspadapter.tenant.TenantConfig;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClientConfigurationUtilTest {

    @Mock
    private TenantConfig tenantConfig;

    @Before
    public void setUp() throws Exception {
      when(tenantConfig.getSphereClientConfig())
        .thenReturn(SphereClientConfig.of("test-key", "test-client-id", "test-client-secret"));
    }

    @Test
    public void executeCreateClient_isBlocking() {
        SphereClient sphereClient = ClientConfigurationUtil.createClient(tenantConfig.getSphereClientConfig());
        assertThat(sphereClient instanceof BlockingSphereClient).isTrue();
    }


}
