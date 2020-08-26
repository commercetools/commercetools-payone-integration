package com.commercetools.util;

import com.commercetools.pspadapter.tenant.TenantConfig;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.TestDoubleSphereClientFactory;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.http.HttpStatusCode;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQueryBuilder;
import java.util.concurrent.CompletionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @Test
    public void decorateClient_shouldRetry_when5xxHttpResponse() {
        SphereClient mockSphereUnderlyingClient =
            spy(TestDoubleSphereClientFactory.createHttpTestDouble(
                intent -> HttpResponse.of(HttpStatusCode.BAD_GATEWAY_502)));

        final BlockingSphereClient blockingSphereClient =
            ClientConfigurationUtil.decorateSphereClient(mockSphereUnderlyingClient);

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
    public void decorateClient_shouldNotRetry_when4xxHttpResponse() {
        SphereClient mockSphereUnderlyingClient =
                spy(TestDoubleSphereClientFactory.createHttpTestDouble(
                        intent -> HttpResponse.of(HttpStatusCode.BAD_REQUEST_400)));

        final BlockingSphereClient blockingSphereClient =
                ClientConfigurationUtil.decorateSphereClient(mockSphereUnderlyingClient);

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
