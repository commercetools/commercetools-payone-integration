package com.commercetools.pspadapter.payone;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsApiException;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.payments.Payment;
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

    @Test(expected = NullPointerException.class)
    public void testGetPaymentsSince() throws CommercetoolsApiException {
        PaymentQueryExecutor paymentQueryExecutor = new PaymentQueryExecutor(client);

        List<Payment> paymentList = new ArrayList<>(paymentQueryExecutor.getPaymentsSince(currentDate));
        assertThat(paymentList, hasSize(1));
    }

    private SphereClient getSphereClientDoubleWithJson() {
        return SphereClientFactory.createHttpTestDouble(httpRequest -> {
            final HttpResponse response;
            if (httpRequest.getPath().startsWith("/payments?where=")) {
                String decodedParams = URLDecoder.decode(httpRequest.getPath());
                assertThat(decodedParams, containsString("createdAt >= \"" + currentDate.toString() + "\""));
                //JSON representation is often useful to deal with errors, but this time again a happy path example
                //alternatively you can provide the String from a file in the classpath
                response = HttpResponse.of(200, " {\n" +
                        "      \"id\": \"d82bc081-771e-4da4-bf27-7e947eb03b6b\",\n" +
                        "      \"version\": 1,\n" +
                        "      \"amountPlanned\": {\n" +
                        "        \"currencyCode\": \"EUR\",\n" +
                        "        \"centAmount\": 20000\n" +
                        "      },\n" +
                        "      \"paymentMethodInfo\": {},\n" +
                        "      \"paymentStatus\": {},\n" +
                        "      \"transactions\": [\n" +
                        "        {\n" +
                        "          \"id\": \"585be1ad-cb16-44b0-947e-a4c41c3af0c6\",\n" +
                        "          \"timestamp\": \"" + currentDate.plusMinutes(5).toString() + "\",\n" +
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
                        "      \"createdAt\": \"" + currentDate.plusMinutes(5).toString() + "\",\n" +
                        "      \"lastModifiedAt\": \"" + currentDate.plusMinutes(5).toString() + "\",\n" +
                        "      \"lastMessageSequenceNumber\": 1\n" +
                        "    }");
            } else {
                //here you can put if else blocks for further preconfigured responses
                throw new UnsupportedOperationException("I'm not prepared for this request: " + httpRequest);
            }
            return response;
        });
    }
}