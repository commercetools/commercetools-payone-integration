package com.commercetools.main;

import com.commercetools.Main;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl.executeGetRequest;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static util.PaymentRequestHelperUtil.CTP_CLIENT;
import static util.PaymentRequestHelperUtil.PAYMENT_STATUS_APPROVED;
import static util.PaymentRequestHelperUtil.TRANSACTION_STATUS_SUCCESS;
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
        testInternalProperties.put("TEST_DATA_PAYONE_KEY", getPayoneKey());
        testInternalProperties.put("TEST_DATA_PAYONE_MERCHANT_ID", getPayoneMerchantId());
        testInternalProperties.put("TEST_DATA_PAYONE_PORTAL_ID", getPayonePortalId());
        testInternalProperties.put("TEST_DATA_PAYONE_SUBACC_ID", getPayoneSubAccId());

        testInternalProperties.put("FIRST_TENANT_CT_PROJECT_KEY", getProjectKey());
        testInternalProperties.put("FIRST_TENANT_CT_CLIENT_ID", getClientId());
        testInternalProperties.put("FIRST_TENANT_CT_CLIENT_SECRET", getClientSecret());
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
        // Prepare
        final String paymentId = createPayment("CREDIT_CARD", "40", "EUR");

        // Test
        final HttpResponse httpResponse = executeGetRequest(format(URL_HANDLE_PAYMENT+paymentId, DEFAULT_PORT));

        // Assert
        assertThat(httpResponse.getStatusLine().getStatusCode()).isIn(HttpStatus.SC_ACCEPTED, HttpStatus.SC_OK);

        final PagedQueryResult<Payment> paymentPagedQueryResult =
            CTP_CLIENT.execute(PaymentQuery.of()
                                           .withPredicates(QueryPredicate.of(format("id=\"%s\"", paymentId))))
                      .toCompletableFuture().join();

        assertThat(paymentPagedQueryResult.getResults().get(0))
            .satisfies(
                payment -> {
                    assertThat(payment.getPaymentStatus().getInterfaceCode()).isEqualTo(PAYMENT_STATUS_APPROVED);
                    assertThat(payment.getTransactions().get(0).getState()).isEqualTo(TRANSACTION_STATUS_SUCCESS);
                });
    }
}
