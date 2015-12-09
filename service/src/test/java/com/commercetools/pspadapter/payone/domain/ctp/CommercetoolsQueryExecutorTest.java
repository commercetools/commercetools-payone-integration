package com.commercetools.pspadapter.payone.domain.ctp;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.messages.PaymentCreatedMessage;
import io.sphere.sdk.payments.queries.PaymentQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.utils.IOUtils;
import util.SphereClientDoubleCreator;

import java.io.IOException;
import java.io.InputStream;
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
public class CommercetoolsQueryExecutorTest {

    private static final Logger LOG = LoggerFactory.getLogger(CommercetoolsQueryExecutorTest.class);

    private CommercetoolsClient client;
    private CommercetoolsQueryExecutor commercetoolsQueryExecutor;

    @Before
    public void setUp() throws Exception {
        client = new CommercetoolsClient(SphereClientDoubleCreator
                .getSphereClientWithResponseFunction(CommercetoolsQueryExecutorTest::setTestResponses));
        commercetoolsQueryExecutor = new CommercetoolsQueryExecutor(client);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test()
    public void executePaymentQueryByTransactionState() {
        List<Payment> paymentList = new ArrayList<>(commercetoolsQueryExecutor
                                                        .getPaymentsWithTransactionState(TransactionState.SUCCESS));
        assertThat(paymentList, hasSize(1));
        assertThat(paymentList.get(0).getTransactions(), hasSize(1));
        assertThat(paymentList.get(0).getTransactions().get(0).getState(), is(TransactionState.SUCCESS));
    }

    @Test()
    @Ignore
    public void executePaymentCreatedMessagesQuery() {
        ZonedDateTime since = ZonedDateTime.of(2015, 12, 3, 10, 0, 0, 0, ZoneId.systemDefault());
        List<PaymentCreatedMessage> paymentCreatedMessageList = new ArrayList<>(commercetoolsQueryExecutor
                .getPaymentCreatedMessages(since, TransactionState.PENDING));
        assertThat(paymentCreatedMessageList, hasSize(1));
        assertThat(paymentCreatedMessageList.get(0).getPayment(), is(notNullValue()));
        assertThat(paymentCreatedMessageList.get(0).getPayment().getTransactions(), hasSize(1));
        assertThat(paymentCreatedMessageList.get(0).getPayment().getTransactions().get(0).getState(), is(TransactionState.PENDING));
    }

    @Test
    public void executeOrderQueryById() {
        Order orderResult = commercetoolsQueryExecutor.getOrderById("dd777a1f-e5e7-467d-a846-1d5b24437768");
        assertThat(orderResult.getId(), is("dd777a1f-e5e7-467d-a846-1d5b24437768"));
    }

    private static Object setTestResponses(HttpRequestIntent intent) {
        try {
            if (intent.getPath().startsWith("/orders")) {
                //response for OrderByIdGet
                InputStream dummyOrderJson = getJsonFromFile("dummyOrder.json");
                return SphereJsonUtils.readObject(IOUtils.toString(dummyOrderJson), Order.typeReference());
            }
            else if (intent.getPath().startsWith("/payments?where=")) {
                //response for PaymentQuery with transaction state
                String decodedParams = URLDecoder.decode(intent.getPath(), "UTF-8");
                assertThat(decodedParams, containsString("transactions(state = \"" + TransactionState.SUCCESS.toSphereName() + "\""));
                InputStream dummyPaymentJson = getJsonFromFile("dummyPayment.json");
                return SphereJsonUtils.readObject(IOUtils.toString(dummyPaymentJson), PaymentQuery.resultTypeReference());
            }
            else if (intent.getPath().startsWith("/messages")) {
                String decodedParams = URLDecoder.decode(intent.getPath(), "UTF-8");
                assertThat(decodedParams, containsString("transactions(state = \"" + TransactionState.PENDING.toSphereName() + "\""));
                //TODO: return PaymentCreatedMessage reference type!
            }
            else {
                throw new UnsupportedOperationException("I'm not prepared for this request: " + intent);
            }
        } catch (Exception ex) {
            LOG.error("Error during creation of dummy response.", ex);
        }
        return null;
    }

    private static InputStream getJsonFromFile(String filePath) throws IOException {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
    }
}