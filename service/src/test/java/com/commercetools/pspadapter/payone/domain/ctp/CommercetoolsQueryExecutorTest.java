package com.commercetools.pspadapter.payone.domain.ctp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.orders.Order;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.utils.IOUtils;
import util.SphereClientDoubleCreator;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author fhaertig
 * @date 03.12.15
 */
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