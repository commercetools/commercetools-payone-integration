package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.tenant.TenantFactory;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereTimeoutException;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.commercetools.pspadapter.payone.IntegrationService.*;
import static com.google.common.collect.ImmutableList.of;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationServiceTest {

    private static final String APPLICATION_VERSION = "1.0";
    private static final String APPLICATION_TITLE = "applicationTitle";
    private static final String TENANTNAME1 = "tenant1";
    private static final String TENANTNAME2 = "tenant2";

    IntegrationService integrationService = new IntegrationService();

    @Test
    public void createHealthResponse_tenantConfigIsWorking_shouldReturnOkStatus() {
        List<TenantFactory> tenantFactories = of(createTenantFactory(TENANTNAME1, false),
                createTenantFactory(TENANTNAME2, false));
        ImmutableMap<String, Object> result = integrationService.createHealthResponse(APPLICATION_VERSION,
                APPLICATION_TITLE, tenantFactories);
        assertThat(result.get(STATUS_KEY)).isEqualTo(SUCCESS_STATUS);
        Map<String, Integer> statusMap=(Map)result.get(TENANTS_KEY);
        assertThat(statusMap.get(TENANTNAME1)).isEqualTo(SUCCESS_STATUS);
        assertThat(statusMap.get(TENANTNAME2)).isEqualTo(SUCCESS_STATUS);
        final ImmutableMap<String, String> applicationInfoMap =(ImmutableMap)result.get(APPLICATIONINFO_KEY);
        assertThat(applicationInfoMap.get("version")).isEqualTo(APPLICATION_VERSION);
        assertThat(applicationInfoMap.get("title")).isEqualTo(APPLICATION_TITLE);
    }

    @Test
    public void createHealthResponse_allTenantsAreNotWorking_shouldReturnErrorStatus() {
        List<TenantFactory> tenantFactories = of(createTenantFactory(TENANTNAME1, true),
                createTenantFactory(TENANTNAME2, true));
        ImmutableMap<String, Object> result = integrationService.createHealthResponse(APPLICATION_VERSION,
                APPLICATION_TITLE, tenantFactories);
        assertThat(result.get(STATUS_KEY)).isEqualTo(ERROR_STATUS);
        Map<String, Integer> statusMap=(Map)result.get(TENANTS_KEY);
        assertThat(statusMap.get(TENANTNAME1)).isEqualTo(ERROR_STATUS);
        assertThat(statusMap.get(TENANTNAME2)).isEqualTo(ERROR_STATUS);
        final ImmutableMap<String, String> applicationInfoMap =(ImmutableMap)result.get(APPLICATIONINFO_KEY);
        assertThat(applicationInfoMap.get("version")).isEqualTo(APPLICATION_VERSION);
        assertThat(applicationInfoMap.get("title")).isEqualTo(APPLICATION_TITLE);
    }

    @Test
    public void createHealthResponse_oneTenantIsNotWorking_shouldReturnErrorStatus() {
        List<TenantFactory> tenantFactories = of(createTenantFactory(TENANTNAME1, true),
                createTenantFactory(TENANTNAME2, false));
        ImmutableMap<String, Object> result = integrationService.createHealthResponse(APPLICATION_VERSION,
                APPLICATION_TITLE, tenantFactories);
        assertThat(result.get(STATUS_KEY)).isEqualTo(ERROR_STATUS);
        Map<String, Integer> statusMap=(Map)result.get(TENANTS_KEY);
        assertThat(statusMap.get(TENANTNAME1)).isEqualTo(ERROR_STATUS);
        assertThat(statusMap.get(TENANTNAME2)).isEqualTo(SUCCESS_STATUS);
        final ImmutableMap<String, String> applicationInfoMap =(ImmutableMap)result.get(APPLICATIONINFO_KEY);
        assertThat(applicationInfoMap.get("version")).isEqualTo(APPLICATION_VERSION);
        assertThat(applicationInfoMap.get("title")).isEqualTo(APPLICATION_TITLE);
    }

    public TenantFactory createTenantFactory(String tenantName, boolean erroneous) {
        TenantFactory tenantFactory = Mockito.mock(TenantFactory.class);
        BlockingSphereClient client = Mockito.mock(BlockingSphereClient.class);
        if (erroneous) {
            Mockito.when(client.execute(Mockito.anyObject())).thenReturn(exceptionallyCompletedFuture(new SphereTimeoutException(new TimeoutException())));
        } else {
            PagedQueryResult mockResult = Mockito.mock(PagedQueryResult.class);
            Mockito.when(client.execute(Mockito.anyObject())).thenReturn(completedFuture(mockResult));
        }
        Mockito.when(tenantFactory.getBlockingSphereClient()).thenReturn(client);
        Mockito.when(tenantFactory.getTenantName()).thenReturn(tenantName);
        return tenantFactory;
    }
}