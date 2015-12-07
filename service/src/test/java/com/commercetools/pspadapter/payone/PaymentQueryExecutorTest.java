package com.commercetools.pspadapter.payone;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsApiException;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.queries.OrderQuery;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.queries.PaymentQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import util.SphereClientDoubleCreator;

import java.io.UnsupportedEncodingException;
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

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test()
    public void executePaymentQueryByTransactionState() throws CommercetoolsApiException {
        client = new CommercetoolsClient(SphereClientDoubleCreator
                .getSphereClientWithPaymentRoute(PaymentQueryExecutorTest::setPaymentResponse));
        PaymentQueryExecutor paymentQueryExecutor = new PaymentQueryExecutor(client);

        List<Payment> paymentList = new ArrayList<>(paymentQueryExecutor
                                                        .getPaymentsWithTransactionState(TransactionState.SUCCESS));
        assertThat(paymentList, hasSize(1));
        assertThat(paymentList.get(0).getTransactions(), hasSize(1));
        assertThat(paymentList.get(0).getTransactions().get(0).getState(), is(TransactionState.SUCCESS));
    }


    public void executeOrderQueryById() throws CommercetoolsApiException {
        client = new CommercetoolsClient(SphereClientDoubleCreator
                .getSphereClientWithOrderRoute(PaymentQueryExecutorTest::setOrderResponse));
        PaymentQueryExecutor paymentQueryExecutor = new PaymentQueryExecutor(client);

        Order orderResult = paymentQueryExecutor.getOrderById("123");
        assertThat(orderResult.getId(), is("123"));
    }

    private static Object setPaymentResponse(HttpRequestIntent intent) {
        try {
            String decodedParams = URLDecoder.decode(intent.getPath(), "UTF-8");
            assertThat(decodedParams, containsString("transactions(state = \"" + TransactionState.SUCCESS.toSphereName() + "\""));
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
                    "      \"createdAt\": \"2015-12-03T10:00:31.498Z\",\n" +
                    "      \"lastModifiedAt\": \"2015-12-03T10:00:31.498Z\",\n" +
                    "      \"lastMessageSequenceNumber\": 1\n" +
                    "    }]\n" +
                    "}";
            return SphereJsonUtils.readObject(dummyPayment, PaymentQuery.resultTypeReference());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Object setOrderResponse(HttpRequestIntent intent) {
        final String dummyOrder =
                "    {\n" +
                "      \"id\": \"123\"\n" +
                "    }";
        return SphereJsonUtils.readObject(dummyOrder, OrderQuery.resultTypeReference());
    }
}