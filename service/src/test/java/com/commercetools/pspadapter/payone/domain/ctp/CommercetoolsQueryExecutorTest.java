package com.commercetools.pspadapter.payone.domain.ctp;

import com.commercetools.pspadapter.payone.PaymentDispatcher;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.messages.queries.MessageQuery;
import io.sphere.sdk.orders.Order;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.utils.IOUtils;
import util.PaymentTestHelper;
import util.SphereClientDoubleCreator;

import java.io.InputStream;

/**
 * @author fhaertig
 * @date 03.12.15
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore
public class CommercetoolsQueryExecutorTest extends PaymentTestHelper {

    private static final Logger LOG = LoggerFactory.getLogger(CommercetoolsQueryExecutorTest.class);

    private CommercetoolsClient client;
    private CommercetoolsQueryExecutor commercetoolsQueryExecutor;

    @Mock
    private PaymentDispatcher paymentDispatcher;

    @Before
    public void setUp() throws Exception {
        client = new CommercetoolsClient(SphereClientDoubleCreator
                .getSphereClientWithResponseFunction(CommercetoolsQueryExecutorTest::setTestResponses));
        commercetoolsQueryExecutor = new CommercetoolsQueryExecutor(client);
    }

    //currently there seems to be no way to mock the message endpoint correctly since we can't create e.g.
    //PaymentCreatedMessages from json (missing TypeReference)

    private static Object setTestResponses(HttpRequestIntent intent) {
        try {
            if (intent.getPath().startsWith("/orders")) {
                //response for OrderByIdGet
                InputStream dummyOrderJson = getJsonFromFile("dummyOrder.json");
                return SphereJsonUtils.readObject(IOUtils.toString(dummyOrderJson), Order.typeReference());
            }
            else if (intent.getPath().startsWith("/messages")) {
                InputStream dummyOrderJson = getJsonFromFile("dummyPaymentCreatedMessage.json");
                SphereJsonUtils.readObject(IOUtils.toString(dummyOrderJson), MessageQuery.resultTypeReference());
            }
            else {
                throw new UnsupportedOperationException("I'm not prepared for this request: " + intent);
            }
        } catch (Exception ex) {
            LOG.error("Error during creation of dummy response.", ex);
        }
        return null;
    }
}