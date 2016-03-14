package com.commercetools.pspadapter.payone.notification;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.SetCustomer;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Jan Wolter
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationProcessorBaseTest {
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Mock
    private BlockingSphereClient client;

    @Mock
    private Payment payment;

    @Captor
    private ArgumentCaptor<PaymentUpdateCommand> paymentUpdateCommandArgumentCaptor;

    @Test
    public void rethrowsSphereSdkConcurrentModificationExceptionWrappedInJavaUtilConcurrentModificationException() {
        // arrange
        final NotificationAction txAction = randomTxAction();

        final Notification notification = new Notification();
        notification.setTxaction(txAction);

        final ImmutableList<UpdateAction<Payment>> updateActions = ImmutableList.of(SetCustomer.of(null));

        final NotificationProcessorBase testee = new NotificationProcessorBase(client) {
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
        when(client.executeBlocking(isA(PaymentUpdateCommand.class))).thenThrow(sdkException);

        // act
        final Throwable throwable =
                catchThrowable(() -> testee.processTransactionStatusNotification(notification, payment));

        // assert
        verify(client).executeBlocking(paymentUpdateCommandArgumentCaptor.capture());
        softly.assertThat(paymentUpdateCommandArgumentCaptor.getValue().getUpdateActions()).as("update action instance")
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
