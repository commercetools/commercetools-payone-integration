package com.commercetools.pspadapter.payone.notification;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.commands.updateactions.SetCustomer;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Jan Wolter
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationProcessorBaseTest extends BaseNotificationProcessorTest {
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Mock
    private Payment payment;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void rethrowsSphereSdkConcurrentModificationExceptionWrappedInJavaUtilConcurrentModificationException() {
        // arrange
        final NotificationAction txAction = randomTxAction();

        final Notification notification = new Notification();
        notification.setTxaction(txAction);

        final List<UpdateAction<Payment>> updateActions = Arrays.asList(SetCustomer.of(null));

        final NotificationProcessorBase testee = new NotificationProcessorBase(tenantFactory, tenantConfig, transactionStateResolver) {
            @Override
            protected boolean canProcess(final Notification notification) {
                return true;
            }

            @Override
            protected List<UpdateAction<Payment>> createPaymentUpdates(final Payment aPayment,
                                                                       final Notification aNotification) {
                return (payment == aPayment) && (notification == aNotification) ? updateActions : Collections.emptyList();
            }
        };

        final ConcurrentModificationException sdkException = new ConcurrentModificationException();
        when(paymentService.updatePayment(any(), any())).thenThrow(sdkException);

        // act
        final Throwable throwable =
                catchThrowable(() -> testee.processTransactionStatusNotification(notification, payment));

        // assert
        verify(paymentService).updatePayment(paymentRequestPayment.capture(), paymentRequestUpdatesCaptor.capture());

        // order service methods should not be called
        // because tenantConfig.isUpdateOrderPaymentState() expected to be "false" by default
        verifyUpdateOrderActionsNotCalled();

        softly.assertThat(paymentRequestPayment.getValue()).isEqualTo(payment);

        softly.assertThat(paymentRequestUpdatesCaptor.getValue()).as("update action instance")
                .isSameAs(updateActions);

        softly.assertThat(throwable).as("exception")
                .isInstanceOf(java.util.ConcurrentModificationException.class)
                .hasCause(sdkException);
    }

    @Test
    public void isNotCompletedTransaction_callsInjectedStateResolver() throws Exception {
        final NotificationProcessorBase testee = new NotificationProcessorBase(tenantFactory, tenantConfig, transactionStateResolver){
            @Override
            protected boolean canProcess(Notification notification) {
                return true;
            }
        };

        doReturn(true).when(transactionStateResolver).isNotCompletedTransaction(any());
        assertThat(testee.isNotCompletedTransaction(mock(Transaction.class))).isTrue();

        doReturn(false).when(transactionStateResolver).isNotCompletedTransaction(any());
        assertThat(testee.isNotCompletedTransaction(mock(Transaction.class))).isFalse();
    }

    private static NotificationAction randomTxAction() {
        final List<NotificationAction> txActions = Arrays.asList(NotificationAction.values());
        Collections.shuffle(txActions);
        return txActions.get(0);
    }
}
