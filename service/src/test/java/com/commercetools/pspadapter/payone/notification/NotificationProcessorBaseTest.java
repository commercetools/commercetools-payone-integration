package com.commercetools.pspadapter.payone.notification;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.commands.updateactions.SetCustomer;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        final ImmutableList<UpdateAction<Payment>> updateActions = ImmutableList.of(SetCustomer.of(null));

        final NotificationProcessorBase testee = new NotificationProcessorBase(serviceFactory) {
            @Override
            protected boolean canProcess(final Notification notification) {
                return true;
            }

            @Override
            protected ImmutableList<UpdateAction<Payment>> createPaymentUpdates(final Payment aPayment,
                                                                                final Notification aNotification) {
                return (payment == aPayment) && (notification == aNotification) ? updateActions : ImmutableList.of();
            }
        };

        final ConcurrentModificationException sdkException = new ConcurrentModificationException();
        when(paymentService.updatePayment(any(), any())).thenThrow(sdkException);

        // act
        final Throwable throwable =
                catchThrowable(() -> testee.processTransactionStatusNotification(notification, payment));

        // assert
        verify(paymentService).updatePayment(paymentRequestPayment.capture(), paymentRequestUpdatesCaptor.capture());

        final Payment updatePayment = paymentRequestPayment.getValue();
        assertThat(updatePayment).isEqualTo(payment);

        softly.assertThat(paymentRequestUpdatesCaptor.getValue()).as("update action instance")
                .isSameAs(updateActions);

        softly.assertThat(throwable).as("exception")
                .isInstanceOf(java.util.ConcurrentModificationException.class)
                .hasCause(sdkException);
    }

    private static NotificationAction randomTxAction() {
        final ArrayList<NotificationAction> txActions = Lists.newArrayList(NotificationAction.values());
        Collections.shuffle(txActions);
        return txActions.get(0);
    }
}
