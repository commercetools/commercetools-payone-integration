package com.commercetools.pspadapter.payone;

import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.sameOrAfter;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsApiException;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.queries.PaymentQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import util.SphereClientDoubleCreator;

import java.net.URLDecoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fhaertig
 * @date 03.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class PaymentQueryExecutorTest {

    private CommercetoolsClient client;

    private static ZonedDateTime testDate = ZonedDateTime.of(2015, 10, 1, 8, 0, 0, 0, ZoneId.systemDefault());

    @Before
    public void setUp() throws Exception {
        client = new CommercetoolsClient(SphereClientDoubleCreator
                .getSphereClientWithPaymentRoute(PaymentQueryExecutorTest::setClientResponse));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test()
    public void executePaymentQueryByTimeLimit() throws CommercetoolsApiException {
        PaymentQueryExecutor paymentQueryExecutor = new PaymentQueryExecutor(client);

        List<Payment> paymentList = new ArrayList<>(paymentQueryExecutor.getPaymentsSince(testDate));
        assertThat(paymentList, hasSize(1));
        assertThat(paymentList.get(0).getCreatedAt(), sameOrAfter(testDate));
    }

    private static Object setClientResponse(HttpRequestIntent intent) {
        String decodedParams = URLDecoder.decode(intent.getPath());
        assertThat(decodedParams, containsString("createdAt >= \"" + testDate.toString() + "\""));
        //JSON representation is often useful to deal with errors, but this time again a happy path example
        //alternatively you can provide the String from a file in the classpath
        final String dummyPayment = "{\n" +
                "  \"offset\": 0,\n" +
                "  \"count\": 1,\n" +
                "  \"total\": 1,\n" +
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
                "      \"createdAt\": \"" + testDate.plusMinutes(5).toString() + "\",\n" +
                "      \"lastModifiedAt\": \""+ testDate.plusMinutes(5).toString() + "\",\n" +
                "      \"lastMessageSequenceNumber\": 1\n" +
                "    }]\n" +
                "}";
        return SphereJsonUtils.readObject(dummyPayment, PaymentQuery.resultTypeReference());
    }
}