package com.commercetools.main;

import com.commercetools.Main;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import static util.PaymentRequestHelperUtil.URL_HANDLE_PAYMENT;
import static util.PaymentRequestHelperUtil.cleanupData;
import static util.PaymentRequestHelperUtil.createPayment;
import static util.PropertiesHelperUtil.getClientId;
import static util.PropertiesHelperUtil.getClientSecret;
import static util.PropertiesHelperUtil.getPayoneKey;
import static util.PropertiesHelperUtil.getPayoneMerchantId;
import static util.PropertiesHelperUtil.getPayonePortalId;
import static util.PropertiesHelperUtil.getPayoneSubAccId;
import static util.PropertiesHelperUtil.getProjectKey;
import static util.PropertiesHelperUtil.getTenant;

public class BasicPaymentRequestWorkflow {

    public static final int DEFAULT_PORT = 8080;
    private final Map<String, String> testInternalProperties = new HashMap<>();

    @Before
    public void setUp() throws URISyntaxException {
        cleanupData();
        testInternalProperties.put("TENANTS", getTenant());
        testInternalProperties.put("TEST_DATA_CT_PROJECT_KEY", getProjectKey());
        testInternalProperties.put("TEST_DATA_CT_CLIENT_ID", getClientId());
        testInternalProperties.put("TEST_DATA_CT_CLIENT_SECRET", getClientSecret());

//        testInternalProperties.put("TEST_DATA_PAYONE_KEY", "TvIOwFdSzOSVKE3Y");
//        testInternalProperties.put("TEST_DATA_PAYONE_MERCHANT_ID", "31102");
//        testInternalProperties.put("TEST_DATA_PAYONE_PORTAL_ID", "2022125");
//        testInternalProperties.put("TEST_DATA_PAYONE_SUBACC_ID", "31281");

        testInternalProperties.put("FIRST_TENANT_PAYONE_KEY", getPayoneKey());
        testInternalProperties.put("FIRST_TENANT_PAYONE_SUBACC_ID", getPayoneSubAccId());
        testInternalProperties.put("FIRST_TENANT_PAYONE_MERCHANT_ID", getPayoneMerchantId());
        testInternalProperties.put("FIRST_TENANT_PAYONE_PORTAL_ID", getPayonePortalId());


        PropertyProvider propertyProvider = Main.getPropertyProvider();

        List<Function<String, String>> propertiesGetters = propertyProvider.getPropertiesGetters();
        propertiesGetters.add(testInternalProperties::get);
        Main.main(new String[0]);
    }

    @Test
    public void verifyCreditCardTransferPayoneRequestIsSuccess() throws Exception {
        final String paymentId = createPayment("CREDIT_CARD", "CHARGE", "40", "EUR");

        HttpResponse httpResponse = executeGetRequest(format(URL_HANDLE_PAYMENT+paymentId, DEFAULT_PORT));
        assertThat(httpResponse.getStatusLine().getStatusCode()).isIn(HttpStatus.SC_ACCEPTED, HttpStatus.SC_OK);
    }
}
