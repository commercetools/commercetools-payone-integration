package com.commercetools.pspadapter.payone.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

/**
 * @author fhaertig
 * @author Jan Wolter
 * @since 05.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationDispatcherTest {

    private static final String dummyInterfaceId = "123";

    @Mock
    private CommercetoolsClient client;

    @Mock
    private PropertyProvider propertyProvider;

    @Mock
    private NotificationProcessor defaultNotificationProcessor;

    @Mock
    private NotificationProcessor specificNotificationProcessor;

    private final PaymentTestHelper testHelper = new PaymentTestHelper();

    private PayoneConfig config;

    private ImmutableMap<NotificationAction, NotificationProcessor> processors;

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

        processors = ImmutableMap.of(NotificationAction.APPOINTED, specificNotificationProcessor);
    }

    @Test
    public void dispatchValidAppointedNotification() {
        // arrange
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

        final NotificationDispatcher dispatcher =
                new NotificationDispatcher(defaultNotificationProcessor, processors, client, config);

        // act
        dispatcher.dispatchNotification(notification);

        // assert
        // TODO check for specific payment instead of "any"
        verify(specificNotificationProcessor).processTransactionStatusNotification(same(notification), any());
        verifyZeroInteractions(defaultNotificationProcessor);
    }

    @Test
    public void retriesToDispatchValidAppointedNotificationInCaseOfConcurrentModificationException() {
        // arrange
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

        final NotificationDispatcher dispatcher =
                new NotificationDispatcher(defaultNotificationProcessor, processors, client, config);

        doThrow(new ConcurrentModificationException("payment modified concurrently")) // 1st try throws
                .doNothing() // will succeed from 2nd try on
                .when(specificNotificationProcessor).processTransactionStatusNotification(same(notification), any());

        // act
        dispatcher.dispatchNotification(notification);

        // assert
        // TODO check for specific payment instead of "any"
        verify(specificNotificationProcessor, times(2)).processTransactionStatusNotification(same(notification), any());
        verifyZeroInteractions(defaultNotificationProcessor);
    }

    @Test
    public void refuseNotificationWithWrongSecrets() {
        // arrange
        final Notification notification = new Notification();
        notification.setTxid("123");
        notification.setPortalid("invalidPortal");
        notification.setTxtime("1450365542");
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);

        final NotificationDispatcher dispatcher =
                new NotificationDispatcher(defaultNotificationProcessor, processors, client, config);

        // act
        final Throwable throwable = catchThrowable(() -> dispatcher.dispatchNotification(notification));

        // assert
        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not valid for this service instance");

        verifyZeroInteractions(specificNotificationProcessor, defaultNotificationProcessor);
    }

    @Test
    public void dispatchNotificationToNewPayment() {
        // arrange
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

        final NotificationDispatcher dispatcher =
                new NotificationDispatcher(defaultNotificationProcessor, processors, client, config);

        // act
        dispatcher.dispatchNotification(notification);

        // assert
        // TODO check for specific payment instead of "any"
        verify(specificNotificationProcessor).processTransactionStatusNotification(same(notification), any());
        verifyZeroInteractions(defaultNotificationProcessor);
    }
}
