package com.commercetools.util;

import com.commercetools.pspadapter.tenant.TenantConfig;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.TestDoubleSphereClientFactory;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.http.HttpStatusCode;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQueryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(GenericClientConfigurationUtil.class)
public class ClientConfigurationUtilTest {

    @Mock
    private TenantConfig tenantConfig;

    static {
        mockStatic(GenericClientConfigurationUtil.class);
    }

    @Before
    public void setUp() throws Exception {
        when(tenantConfig.getSphereClientConfig())
            .thenReturn(SphereClientConfig.of("test-key", "test-client-id", "test-client-secret"));
    }

    @Test
    public void createClient_isBlocking() {
        SphereClient sphereClient = ClientConfigurationUtil.createClient(tenantConfig.getSphereClientConfig());
        assertThat(sphereClient instanceof BlockingSphereClient).isTrue();
    }

    @Test
    public void createClient_shouldRetry_when5xxHttpResponse() {
        SphereClientConfig sphereClientConfig = tenantConfig.getSphereClientConfig();
        SphereClient mockSphereUnderlyingClient = spy(
            TestDoubleSphereClientFactory.createHttpTestDouble(
                intent -> HttpResponse.of(HttpStatusCode.BAD_GATEWAY_502)));

        when(GenericClientConfigurationUtil.createHttpClient(sphereClientConfig))
            .thenReturn(mockSphereUnderlyingClient);

        final BlockingSphereClient blockingSphereClient = ClientConfigurationUtil.createClient(sphereClientConfig);

        final TaxCategoryQuery query = TaxCategoryQueryBuilder.of().build();
        try {
            blockingSphereClient.execute(query).toCompletableFuture().join();
        } catch (CompletionException e) {
            // Skipped
        }
        verify(mockSphereUnderlyingClient,
                times(ClientConfigurationUtil.RETRIES_LIMIT+1))
            .execute(query);
    }

    @Test
    public void createClient_shouldNotRetry_when4xxHttpResponse() {
        SphereClientConfig sphereClientConfig = tenantConfig.getSphereClientConfig();
        SphereClient mockSphereUnderlyingClient = spy(
                TestDoubleSphereClientFactory.createHttpTestDouble(
                        intent -> HttpResponse.of(HttpStatusCode.BAD_REQUEST_400)));

        when(GenericClientConfigurationUtil.createHttpClient(sphereClientConfig))
            .thenReturn(mockSphereUnderlyingClient);

        final BlockingSphereClient blockingSphereClient = ClientConfigurationUtil.createClient(sphereClientConfig);

        final TaxCategoryQuery query = TaxCategoryQueryBuilder.of().build();
        try {
            blockingSphereClient.execute(query).toCompletableFuture().join();
        } catch (CompletionException e) {
            // Skipped
        }
        verify(mockSphereUnderlyingClient,
                times(1))
                .execute(query);
    }
}
