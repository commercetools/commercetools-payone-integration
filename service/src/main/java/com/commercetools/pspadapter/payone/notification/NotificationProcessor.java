package com.commercetools.pspadapter.payone.notification;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.google.common.collect.ImmutableList;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;

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
     */
    boolean processTransactionStatusNotification(
            final Notification notification,
            final Payment payment);

    ImmutableList<UpdateAction<Payment>> createPaymentUpdates(Payment payment, Notification notification);
}
