package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import io.sphere.sdk.payments.Payment;

/**
 * Processes PAYONE transaction status notifications.
 *
 * @author Jan Wolter
 */
interface NotificationProcessor {
    /**
     * Processes the transaction status {@code notification}.
     *
     * @param notification the PAYONE transaction notification to be processed, not null
     * @param payment the payment for which a transaction status notification shall be processed, not null
     * @return whether the notification was processed successfully, i.e. the notification was persisted in the payment
     */
    boolean processTransactionStatusNotification(final Notification notification, final Payment payment);
}
