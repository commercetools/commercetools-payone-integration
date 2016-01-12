package com.commercetools.pspadapter.payone.notification;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.google.common.collect.ImmutableList;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;

import java.util.concurrent.CompletionException;

/**
 * Processes PAYONE transaction status notifications.
 *
 * @author Jan Wolter
 */
public interface NotificationProcessor {

    NotificationAction supportedNotificationAction();

    /**
     * Processes the transaction status {@code notification}.
     *
     * @param notification the PAYONE transaction notification to be processed, not null
     * @param payment the payment for which a transaction status notification shall be processed, not null
     * @return whether the notification was processed successfully, i.e. the notification was persisted in the payment
     * @throws IllegalArgumentException if notification is not supported by this processor
     * @throws RuntimeException if the payment update fails
     * @throws CompletionException if the payment update fails
     */
    boolean processTransactionStatusNotification(
            final Notification notification,
            final Payment payment);

    /**
     * Generates a list of update actions which can be applied to the payment in one step.
     *
     * @param payment the payment to which the updates may apply
     * @param notification the notification to process
     * @return an immutable list of update actions which will e.g. add an interfaceInteraction to the payment
     * or apply changes to a corresponding transaction in the payment
     */
    ImmutableList<UpdateAction<Payment>> createPaymentUpdates(Payment payment, Notification notification);
}
