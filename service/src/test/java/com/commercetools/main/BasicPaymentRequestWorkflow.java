package com.commercetools.main;

import com.commercetools.MainTestRunner;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl.executeGetRequest;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class BasicPaymentRequestWorkflow {

    private final String CT_PROJECT_KEY = "payone-integration-tests-heroku-17";
    private final String CT_CLIENT_ID = "f4e4AIIfXh0zaC6k7sNkCI4Q";
    private final String CT_CLIENT_SECRET = "zf_o_F0oknJ4ZOqOlhY4LiSmTOHCfX8h";
    private final Map<String, String> testInternalProperties = new HashMap<>();
    private static final String URL_PATTERN = "http://localhost:%d/health";

    @Before
    public void setUp() throws URISyntaxException {
        testInternalProperties.put("TENANTS", "TEST_DATA");
        testInternalProperties.put("TEST_DATA_CT_PROJECT_KEY", CT_PROJECT_KEY);
        testInternalProperties.put("TEST_DATA_CT_CLIENT_ID", CT_CLIENT_ID);
        testInternalProperties.put("TEST_DATA_CT_CLIENT_SECRET", CT_CLIENT_SECRET);
        testInternalProperties.put("TEST_DATA_PAYONE_KEY", "TvIOwFdSzOSVKE3Y");
        testInternalProperties.put("TEST_DATA_PAYONE_MERCHANT_ID", "31102");
        testInternalProperties.put("TEST_DATA_PAYONE_PORTAL_ID", "2022125");
        testInternalProperties.put("TEST_DATA_PAYONE_SUBACC_ID", "31281");

        MainTestRunner runner = MainTestRunner.getInstance();
        PropertyProvider propertyProvider = runner.getPropertyProvider();

        List<Function<String, String>> propertiesGetters = propertyProvider.getPropertiesGetters();
        propertiesGetters.add(testInternalProperties::get);
        runner.startPayoneService();
    }

    //TODO we need to create ctp resources and try to simulate handle payment call
    // to ensure that payone-integration service will work as expected
    @Test
    public void makeBankTransferPayoneRequest() throws IOException {
        HttpResponse httpResponse = executeGetRequest(format(URL_PATTERN, 8080));
        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
    }
}
