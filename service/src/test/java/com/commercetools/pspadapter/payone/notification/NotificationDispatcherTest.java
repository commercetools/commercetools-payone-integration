package com.commercetools.pspadapter.payone.notification;

import com.commercetools.pspadapter.BaseTenantPropertyTest;
import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import com.commercetools.pspadapter.payone.util.PayoneHash;
import com.commercetools.pspadapter.tenant.TenantFactory;
import com.commercetools.service.PaymentServiceImpl;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import util.PaymentTestHelper;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.pspadapter.payone.util.PayoneConstants.PAYONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author fhaertig
 * @author Jan Wolter
 * @since 05.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationDispatcherTest extends BaseTenantPropertyTest {

    private static final String dummyInterfaceId = "123";

    @Mock
    private TenantFactory tenantFactory;

    @Mock(lenient = true)
    private PaymentServiceImpl paymentServiceImpl;

    @Mock
    private NotificationProcessor defaultNotificationProcessor;

    @Mock
    private NotificationProcessor specificNotificationProcessor;

    private final PaymentTestHelper testHelper = new PaymentTestHelper();

    private PayoneConfig config;

    private Map<NotificationAction, NotificationProcessor> processors;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();

        when(tenantFactory.getPayoneInterfaceName()).thenReturn(PAYONE);
        when(tenantFactory.getPaymentService()).thenReturn(paymentServiceImpl);

        when(paymentServiceImpl.getByPaymentMethodAndInterfaceId(anyString(), anyString()))
                .then(a -> {

                    String interfaceId = a.getArgument(1, String.class);
                    Optional<Payment> payment = dummyInterfaceId.equals(interfaceId)
                            ? testHelper.getPaymentQueryResultFromFile("dummyPaymentQueryResult.json").head()
                            : Optional.empty();

                    return CompletableFuture.completedFuture(payment);
                });

        when(paymentServiceImpl.createPayment(anyObject())).then(
                answer -> {
                    PaymentDraft draft = answer.getArgument(0, PaymentDraft.class);
                    Payment payment = dummyInterfaceId.equals(draft.getInterfaceId())
                            ? testHelper.getPaymentQueryResultFromFile("dummyPaymentQueryResult.json").head().get()
                            : testHelper.dummyPaymentCreatedByNotification();

                    return CompletableFuture.completedFuture(payment);
                }
        );

        when(paymentServiceImpl.updatePayment(anyObject(), anyList())).then(
                a -> CompletableFuture.completedFuture(testHelper.getPaymentQueryResultFromFile("dummyPaymentQueryResult.json").head().get())
        );

        when(tenantPropertyProvider.getTenantProperty(any())).thenReturn(Optional.of("dummyConfigValue"));
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(any())).thenReturn("dummyConfigValue");

        config = new PayoneConfig(tenantPropertyProvider);

        processors = Collections.singletonMap(NotificationAction.APPOINTED, specificNotificationProcessor);
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
        notification.setKey(PayoneHash.calculate("dummyConfigValue"));
        notification.setMode("dummyConfigValue");
        notification.setTxtime("1450365542");
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);

        final NotificationDispatcher dispatcher =
                new NotificationDispatcher(defaultNotificationProcessor, processors, tenantFactory, config);

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
        notification.setKey(PayoneHash.calculate("dummyConfigValue"));
        notification.setMode("dummyConfigValue");
        notification.setTxtime("1450365542");
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);

        final NotificationDispatcher dispatcher =
                new NotificationDispatcher(defaultNotificationProcessor, processors, tenantFactory, config);

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
                new NotificationDispatcher(defaultNotificationProcessor, processors, tenantFactory, config);

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
        notification.setKey(PayoneHash.calculate("dummyConfigValue"));
        notification.setMode("dummyConfigValue");
        notification.setTxtime("1450365542");
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);

        final NotificationDispatcher dispatcher =
                new NotificationDispatcher(defaultNotificationProcessor, processors, tenantFactory, config);

        // act
        dispatcher.dispatchNotification(notification);

        // assert
        // TODO check for specific payment instead of "any"
        verify(specificNotificationProcessor).processTransactionStatusNotification(same(notification), any());
        verifyZeroInteractions(defaultNotificationProcessor);
    }
}
