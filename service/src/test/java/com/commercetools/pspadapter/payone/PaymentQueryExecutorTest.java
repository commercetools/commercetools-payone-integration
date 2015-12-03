package com.commercetools.pspadapter.payone;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsApiException;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fhaertig
 * @date 03.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class PaymentQueryExecutorTest {

    private CommercetoolsClient client;

    private DateTime currentDate = new DateTime();

    @Before
    public void setUp() throws Exception {
        client = new CommercetoolsClient(getSphereClientDoubleWithJson());
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test()
    public void executePaymentQueryByTimeLimit() throws CommercetoolsApiException {
        PaymentQueryExecutor paymentQueryExecutor = new PaymentQueryExecutor(client);

        List<Payment> paymentList = new ArrayList<>(paymentQueryExecutor.getPaymentsSince(currentDate));
        assertThat(paymentList, hasSize(1));
    }

    private SphereClient getSphereClientDoubleWithJson() {
        return SphereClientFactory.createObjectTestDouble(httpRequest -> {
            final Object result;
            if (httpRequest.getPath().startsWith("/payments?where=")) {
                String decodedParams = URLDecoder.decode(httpRequest.getPath());
                assertThat(decodedParams, containsString("createdAt >= \"" + currentDate.toString() + "\""));
                //JSON representation is often useful to deal with errors, but this time again a happy path example
                //alternatively you can provide the String from a file in the classpath
                final String dummyPayment = "{\n" +
                        "  \"offset\": 0,\n" +
                        "  \"count\": 8,\n" +
                        "  \"total\": 8,\n" +
                        "  \"results\": [\n" +
                        "    {\n" +
                        "      \"id\": \"40a495a2-d709-4484-88cb-d4129acffda7\",\n" +
                        "      \"version\": 1,\n" +
                        "      \"amountPlanned\": {\n" +
                        "        \"currencyCode\": \"EUR\",\n" +
                        "        \"centAmount\": 20000\n" +
                        "      },\n" +
                        "      \"paymentMethodInfo\": {},\n" +
                        "      \"paymentStatus\": {},\n" +
                        "      \"transactions\": [\n" +
                        "        {\n" +
                        "          \"id\": \"58728a39-c2f5-4467-ab17-b329b55c4423\",\n" +
                        "          \"timestamp\": \"2015-12-03T10:00:31.498Z\",\n" +
                        "          \"type\": \"Charge\",\n" +
                        "          \"amount\": {\n" +
                        "            \"currencyCode\": \"EUR\",\n" +
                        "            \"centAmount\": 20000\n" +
                        "          },\n" +
                        "          \"interactionId\": \"1\",\n" +
                        "          \"state\": \"Success\"\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"interfaceInteractions\": [],\n" +
                        "      \"createdAt\": \"2015-12-03T10:00:32.507Z\",\n" +
                        "      \"lastModifiedAt\": \"2015-12-03T10:00:32.507Z\",\n" +
                        "      \"lastMessageSequenceNumber\": 1\n" +
                        "    }]\n" +
                        "}";
                final PagedQueryResult<Payment> queryResult = SphereJsonUtils.readObject(dummyPayment, PaymentQuery.resultTypeReference());
                result = queryResult;
            } else {
                //here you can put if else blocks for further preconfigured responses
                throw new UnsupportedOperationException("I'm not prepared for this request: " + httpRequest);
            }
            return result;
        });
    }
}