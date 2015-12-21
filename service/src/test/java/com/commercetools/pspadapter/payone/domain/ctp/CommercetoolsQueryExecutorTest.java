package com.commercetools.pspadapter.payone.domain.ctp;

import com.commercetools.pspadapter.payone.PaymentDispatcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.PaymentTestHelper;
import util.SphereClientDoubleCreator;

import java.io.IOException;

/**
 * @author fhaertig
 * @date 03.12.15
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore
public class CommercetoolsQueryExecutorTest {

    private static final Logger LOG = LoggerFactory.getLogger(CommercetoolsQueryExecutorTest.class);
    private final PaymentTestHelper orders = new PaymentTestHelper();

    private CommercetoolsClient client;
    private CommercetoolsQueryExecutor commercetoolsQueryExecutor;

    @Mock
    private PaymentDispatcher paymentDispatcher;

    @Before
    public void setUp() throws Exception {
        client = new CommercetoolsClient(SphereClientDoubleCreator
                .getSphereClientWithResponseFunction((intent) -> {
                    try {
                        if (intent.getPath().startsWith("/orders")) {
                            return orders.getOrderFromFile("dummyOrder.json");
                        } else if (intent.getPath().startsWith("/messages")) {
                            return orders.getOrderFromFile("dummyPaymentCreatedMessage.json");
                        }
                    } catch (final IOException e) {
                        throw new RuntimeException(String.format("Failed to load resource: %s", intent), e);
                    }
                    throw new UnsupportedOperationException(
                            String.format("I'm not prepared for this request: %s", intent));
                }));
        commercetoolsQueryExecutor = new CommercetoolsQueryExecutor(client);
    }

    //currently there seems to be no way to mock the message endpoint correctly since we can't create e.g.
    //PaymentCreatedMessages from json (missing TypeReference)
}