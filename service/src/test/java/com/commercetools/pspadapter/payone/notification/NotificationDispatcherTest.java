package com.commercetools.pspadapter.payone.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author fhaertig
 * @since 05.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationDispatcherTest {

    private static final String dummyInterfaceId = "123";

    @Mock
    private CommercetoolsClient client;

    private final PaymentTestHelper testHelper = new PaymentTestHelper();

    @Mock
    private PropertyProvider propertyProvider;

    private PayoneConfig config;

    private ImmutableMap<NotificationAction, NotificationProcessor> processors;

    private CountingNotificationProcessor defaultNotificationProcessor;

    @Before
    public void setUp() throws Exception {
        when(client.complete(any(PaymentQuery.class)))
                .then(a -> {
                    List arguments = Arrays.asList(a.getArguments());
                    try {
                        //client is asked to get matching payments
                        if (((PaymentQuery) arguments.get(0)).predicates()
                                .stream()
                                .filter(f -> f.equals(dummyInterfaceId))
                                .findFirst().isPresent()) {
                            return testHelper.getPaymentQueryResultFromFile("dummyPaymentQueryResult.json");
                        } else {
                            return PagedQueryResult.empty();
                        }
                    } catch (ClassCastException ex) {
                        //client is asked to create payment
                        if (arguments.get(0).toString().contains("interfaceId=123"))
                        {
                            return testHelper.getPaymentQueryResultFromFile("dummyPaymentQueryResult.json").head().get();
                        } else {
                            return testHelper.dummyPaymentCreatedByNotification();
                        }
                    }
                });

        when(propertyProvider.getProperty(any())).thenReturn(Optional.of("dummyConfigValue"));
        when(propertyProvider.getMandatoryNonEmptyProperty(any())).thenReturn("dummyConfigValue");

        config = new PayoneConfig(propertyProvider);

        processors = ImmutableMap.<NotificationAction, NotificationProcessor>builder()
                .put(NotificationAction.APPOINTED, createAppointedNotificationProcessor())
                .build();

        defaultNotificationProcessor = createDefaultNotificationProcessor();
    }

    @Test
    public void dispatchValidAppointedNotification() {
        final Notification notification = new Notification();
        //txid = interfaceId -> must match the dummyPaymentQueryResult.json!
        notification.setTxid("123");
        notification.setClearingtype("cc");
        notification.setPrice("200.00");
        notification.setCurrency("EUR");
        notification.setPortalid("dummyConfigValue");
        notification.setAid("dummyConfigValue");
        notification.setKey(Hashing.md5().hashString("dummyConfigValue", Charsets.UTF_8).toString());
        notification.setMode("dummyConfigValue");
        notification.setTxtime("1450365542");
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);

        NotificationDispatcher dispatcher = new NotificationDispatcher(defaultNotificationProcessor, processors, client, config);
        dispatcher.dispatchNotification(notification);

        assertThat(((CountingNotificationProcessor) processors.get(NotificationAction.APPOINTED)).getCount()).isEqualTo(1);
        assertThat(defaultNotificationProcessor.getCount()).isEqualTo(0);
    }

    @Test
    public void refuseNotificationWithWrongSecrets() {
        final Notification notification = new Notification();
        notification.setTxid("123");
        notification.setPortalid("invalidPortal");
        notification.setTxtime("1450365542");
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);

        NotificationDispatcher dispatcher = new NotificationDispatcher(createDefaultNotificationProcessor(), processors, client, config);

        final Throwable throwable = catchThrowable(() -> dispatcher.dispatchNotification(notification));

        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not valid for this service instance");

        assertThat(((CountingNotificationProcessor) processors.get(NotificationAction.APPOINTED)).getCount()).isEqualTo(0);
        assertThat(defaultNotificationProcessor.getCount()).isEqualTo(0);
    }

    @Test
    public void dispatchNotificationToNewPayment() {
        final Notification notification = new Notification();
        //txid = interfaceId -> must NOT match the dummyPaymentQueryResult.json!
        notification.setTxid("456");
        notification.setClearingtype("cc");
        notification.setPrice("200.00");
        notification.setCurrency("EUR");
        notification.setPortalid("dummyConfigValue");
        notification.setAid("dummyConfigValue");
        notification.setKey(Hashing.md5().hashString("dummyConfigValue", Charsets.UTF_8).toString());
        notification.setMode("dummyConfigValue");
        notification.setTxtime("1450365542");
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);

        NotificationDispatcher dispatcher = new NotificationDispatcher(defaultNotificationProcessor, processors, client, config);
        dispatcher.dispatchNotification(notification);

        assertThat(((CountingNotificationProcessor) processors.get(NotificationAction.APPOINTED)).getCount()).isEqualTo(1);
        assertThat(defaultNotificationProcessor.getCount()).isEqualTo(0);
    }

    private CountingNotificationProcessor createAppointedNotificationProcessor() {
        return new CountingNotificationProcessor() {
            int count = 0;

            @Override
            public int getCount() {
                return count;
            }

            @Override
            public void processTransactionStatusNotification(final Notification notification, final Payment payment) {
                if (notification.getTxid().equals(payment.getInterfaceId())
                        && payment.getPaymentMethodInfo().getPaymentInterface().equals("PAYONE")
                        && notification.getTxaction().equals(NotificationAction.APPOINTED)) {
                    count++;
                }
            }
        };
    }

    private CountingNotificationProcessor createDefaultNotificationProcessor() {
        return new CountingNotificationProcessor() {
            int count = 0;

            @Override
            public int getCount() {
                return count;
            }

            @Override
            public void processTransactionStatusNotification(final Notification notification, final Payment payment) {
                count++;
            }
        };
    }

    private interface CountingNotificationProcessor extends NotificationProcessor {
        int getCount();
    }

}