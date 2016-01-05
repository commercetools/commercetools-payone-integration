package com.commercetools.pspadapter.payone;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.Payment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;
import util.SphereClientDoubleCreator;

/**
 * @author fhaertig
 * @date 05.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationDispatcherTest {


    private CommercetoolsClient client;

    private final PaymentTestHelper testHelper = new PaymentTestHelper();

    private CountingNotificationProcessor countingNotificationProcessor() {
        return new CountingNotificationProcessor() {
            int count = 0;

            @Override
            public int getCount() {
                return count;
            }

            @Override
            public boolean processTransactionStatusNotification(final Notification notification, final Payment payment) {
                if (notification.getTxid().equals(payment.getInterfaceId())) {
                    count++;
                    return true;
                }
                return false;
            }
        };
    }

    @Before
    public void setUp() throws Exception {
        client = new CommercetoolsClient(SphereClientDoubleCreator
                .getSphereClientWithResponseFunction((intent) -> {
                    try {
                        if (intent.getPath().startsWith("/payments")) {
                            return testHelper.getPaymentQueryResultFromFile("dummyPaymentQueryResult.json");
                        }
                    } catch (final Exception e) {
                        throw new RuntimeException(String.format("Failed to load resource: %s", intent), e);
                    }
                    throw new UnsupportedOperationException(
                            String.format("I'm not prepared for this request: %s", intent));
                }));
    }

    @Test
    public void processAppointedNotification() {
        final Notification notification = new Notification();
        notification.setTxid("123");
        notification.setCurrency("EUR");
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);

        final CountingNotificationProcessor countingNotificationProcessor = countingNotificationProcessor();

        NotificationDispatcher dispatcher = new NotificationDispatcher(countingNotificationProcessor, ImmutableMap.of(), client);
        dispatcher.dispatchNotification(notification);

        assertThat(countingNotificationProcessor.getCount()).isEqualTo(1);
    }

    private interface CountingNotificationProcessor extends NotificationProcessor {
        int getCount();
    }

}