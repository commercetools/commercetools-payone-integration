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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;
import util.SphereClientDoubleCreator;

import java.util.Optional;

/**
 * @author fhaertig
 * @date 05.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationDispatcherTest {


    private CommercetoolsClient client;

    private final PaymentTestHelper testHelper = new PaymentTestHelper();

    @Mock
    private PropertyProvider propertyProvider;

    private PayoneConfig config;

    private ImmutableMap<NotificationAction, NotificationProcessor> processors;

    private CountingNotificationProcessor defaultNotificationProcessor;

    private CountingNotificationProcessor createAppointedNotificationProcessor() {
        return new CountingNotificationProcessor() {
            int count = 0;

            @Override
            public int getCount() {
                return count;
            }

            @Override
            public NotificationAction supportedNotificationAction() {
                return NotificationAction.APPOINTED;
            }

            @Override
            public boolean processTransactionStatusNotification(final Notification notification, final Payment payment) {
                if (notification.getTxid().equals(payment.getInterfaceId())
                        && payment.getPaymentMethodInfo().getPaymentInterface().equals("PAYONE")
                        && notification.getTxaction().equals(NotificationAction.APPOINTED)) {
                    count++;
                    return true;
                }
                return false;
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
            public NotificationAction supportedNotificationAction() {
                return NotificationAction.APPOINTED;
            }

            @Override
            public boolean processTransactionStatusNotification(final Notification notification, final Payment payment) {
                count++;
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
        notification.setTxid("123");
        notification.setCurrency("EUR");
        notification.setPortalid("dummyConfigValue");
        notification.setAid("dummyConfigValue");
        notification.setKey(Hashing.md5().hashString("dummyConfigValue", Charsets.UTF_8).toString());
        notification.setMode("dummyConfigValue");
        notification.setTxtime("1450365542");
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);

        NotificationDispatcher dispatcher = new NotificationDispatcher(defaultNotificationProcessor, processors, client, config);
        assertThat(dispatcher.dispatchNotification(notification)).isEqualTo(true);

        assertThat(((CountingNotificationProcessor) processors.get(NotificationAction.APPOINTED)).getCount()).isEqualTo(1);
        assertThat(defaultNotificationProcessor.getCount()).isEqualTo(0);
    }

    @Test
    public void refuseNotificationWithWrongSecrets() {
        final Notification notification = new Notification();
        notification.setTxid("123");
        notification.setCurrency("EUR");
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

    private interface CountingNotificationProcessor extends NotificationProcessor {
        int getCount();

        @Override
        default ImmutableList<UpdateAction<Payment>> createPaymentUpdates(final Payment payment, final Notification notification) {
            return null;
        }
    }

}