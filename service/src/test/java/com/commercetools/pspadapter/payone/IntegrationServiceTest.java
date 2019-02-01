package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.tenant.TenantFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereTimeoutException;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import spark.Spark;
import spark.utils.IOUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.commercetools.pspadapter.payone.IntegrationService.ERROR_STATUS;
import static com.commercetools.pspadapter.payone.IntegrationService.SUCCESS_STATUS;
import static com.google.common.collect.ImmutableList.of;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static spark.Spark.awaitStop;

public class IntegrationServiceTest {

    private static final String APPLICATION_VERSION = "1.0";
    private static final String APPLICATION_TITLE = "applicationTitle";
    private static final String TENANTNAME1 = "tenant1";
    private static final String TENANTNAME2 = "tenant2";
    IntegrationService integrationService = null;

    private ServiceConfig serviceConfig = null;

    private static HealthResponse request() {
        try {
            URL url = new URL("http://localhost:8080/health");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.connect();
            int statusCode = connection.getResponseCode();
            String body = "";
            if (statusCode == 200) {
                body = IOUtils.toString(connection.getInputStream());
            } else {
                body = IOUtils.toString(connection.getErrorStream());
            }
            return new HealthResponse(statusCode, body);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Before
    public void setUp() throws Exception {
        serviceConfig = Mockito.mock(ServiceConfig.class);
        when(serviceConfig.getApplicationName()).thenReturn(APPLICATION_TITLE);
        when(serviceConfig.getApplicationVersion()).thenReturn(APPLICATION_VERSION);
    }

    @After
    public void tearDown() throws Exception {
        integrationService.stop();
        awaitStop();
    }

    @Test
    public void createHealthResponse_tenantConfigIsWorking_shouldReturnOkStatus() {
        List<TenantFactory> tenantFactories = of(createTenantFactory(TENANTNAME1, false),
                createTenantFactory(TENANTNAME2, false));
        integrationService = new IntegrationService(serviceConfig, tenantFactories);
        integrationService.start();

        HealthResponse result = request();
        assertThat(result.status).isEqualTo(SUCCESS_STATUS);
        assertThat(result.body).isEqualTo(expectedBody(SUCCESS_STATUS, SUCCESS_STATUS, SUCCESS_STATUS));
 }

    @Test
    public void createHealthResponse_allTenantsAreNotWorking_shouldReturnErrorStatus() {
        List<TenantFactory> tenantFactories = of(createTenantFactory(TENANTNAME1, true),
                createTenantFactory(TENANTNAME2, true));
        integrationService = new IntegrationService(serviceConfig, tenantFactories);
        integrationService.start();

        HealthResponse result = request();
        assertThat(result.status).isEqualTo(ERROR_STATUS);
        assertThat(result.body).isEqualTo(expectedBody(ERROR_STATUS, ERROR_STATUS, ERROR_STATUS));

    }

    @Test
    public void createHealthResponse_oneTenantIsNotWorking_shouldReturnErrorStatus() {
        List<TenantFactory> tenantFactories = of(createTenantFactory(TENANTNAME1, true),
                createTenantFactory(TENANTNAME2, false));
        integrationService = new IntegrationService(serviceConfig, tenantFactories);
        integrationService.start();

        HealthResponse result = request();
        assertThat(result.status).isEqualTo(ERROR_STATUS);
        assertThat(result.body).isEqualTo(expectedBody(ERROR_STATUS, ERROR_STATUS, SUCCESS_STATUS));

    }

    private TenantFactory createTenantFactory(String tenantName, boolean erroneous) {
        TenantFactory tenantFactory = Mockito.mock(TenantFactory.class);
        BlockingSphereClient client = Mockito.mock(BlockingSphereClient.class);
        if (erroneous) {
            when(client.execute(Mockito.anyObject())).thenReturn(exceptionallyCompletedFuture(new SphereTimeoutException(new TimeoutException())));
        } else {
            PagedQueryResult mockResult = Mockito.mock(PagedQueryResult.class);
            when(client.execute(Mockito.anyObject())).thenReturn(completedFuture(mockResult));
        }
        when(tenantFactory.getBlockingSphereClient()).thenReturn(client);
        when(tenantFactory.getTenantName()).thenReturn(tenantName);
        return tenantFactory;
    }

    private String expectedBody(int globalStatus, int tenant1Status, int tenant2Status) {
        return "{\"status\":" + globalStatus + ",\"tenants\":{\"tenant2\":" + tenant2Status + ",\"tenant1\":" + tenant1Status + "}," +
                "\"applicationInfo\":{\"version\":\"1.0\",\"title\":\"applicationTitle\"}}";
    }

    private static class HealthResponse {

        public final String body;
        public final int status;


        public HealthResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }
}