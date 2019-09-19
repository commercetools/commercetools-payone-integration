package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.tenant.TenantFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import spark.utils.IOUtils;

import java.net.HttpURLConnection;
import java.net.URL;

import static com.commercetools.pspadapter.payone.IntegrationService.SUCCESS_STATUS;
import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static spark.Spark.awaitStop;

public class IntegrationServiceTest {

    private static final String APPLICATION_VERSION = "1.0";
    private static final String APPLICATION_TITLE = "applicationTitle";
    private static final String TENANTNAME1 = "tenant1";
    private static final String TENANTNAME2 = "tenant2";
    private IntegrationService integrationService = null;

    private ServiceConfig serviceConfig = null;

    private static HealthResponse requestHealth() {
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
        integrationService = new IntegrationService(serviceConfig, of(
                createMockTenantFactory(TENANTNAME1),
                createMockTenantFactory(TENANTNAME2)));

        integrationService.start();

        final HealthResponse result = requestHealth();

        assertThat(result.status).isEqualTo(SUCCESS_STATUS);
        assertThat(result.body).isEqualTo(expectedBody(SUCCESS_STATUS, SUCCESS_STATUS, SUCCESS_STATUS));
    }

    private TenantFactory createMockTenantFactory(String tenantName) {
        TenantFactory tenantFactory = Mockito.mock(TenantFactory.class);
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
