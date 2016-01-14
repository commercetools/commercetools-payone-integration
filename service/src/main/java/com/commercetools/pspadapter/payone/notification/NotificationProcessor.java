package com.commercetools.pspadapter.payone.notification;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.payments.Payment;

import java.util.concurrent.CompletionException;

/**
 * Processes PAYONE transaction status notifications.
 *
 * @author Jan Wolter
 */
public interface NotificationProcessor {

    /**
     * Processes the transaction status {@code notification}.
     *
     * @param notification the PAYONE transaction notification to be processed, not null
     * @param payment the payment for which a transaction status notification shall be processed, not null
     *
     * @throws ConcurrentModificationException in case the respective payment could not be updated due to concurrent
     *                                         modifications; a retry at a later time might be successful
     * @throws IllegalArgumentException if notification is not supported by this processor
     * @throws CompletionException if the payment update fails
     * @throws RuntimeException in case of an unexpected (and probably unrecoverable) error
     */
    void processTransactionStatusNotification(final Notification notification, final Payment payment);
}
