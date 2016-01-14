package com.commercetools.pspadapter.payone.notification;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.google.common.collect.ImmutableList;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;

/**
 * Base for notification processor implementations.
 *
 * @author Jan Wolter
 */
public abstract class NotificationProcessorBase implements NotificationProcessor {
    private final BlockingClient client;

    /**
     * Constructor for implementations.
     *
     * @param client the commercetools platform client to update payments
     */
    protected NotificationProcessorBase(final BlockingClient client) {
        this.client = client;
    }

    @Override
    public void processTransactionStatusNotification(final Notification notification, final Payment payment) {
        if (!notification.getTxaction().equals(supportedNotificationAction())) {
            throw new IllegalArgumentException(String.format(
                    "txaction \"%s\" is not supported by %s",
                    notification.getTxaction(),
                    this.getClass().getName()));
        }

        client.complete(PaymentUpdateCommand.of(payment, createPaymentUpdates(payment, notification)));
    }

    /**
     * Gets the PAYONE "txaction" supported by the instance.
     * @return the "txaction"
     */
    protected abstract NotificationAction supportedNotificationAction();

    /**
     * Generates a list of update actions which can be applied to the payment in one step.
     *
     * @param payment the payment to which the updates may apply
     * @param notification the notification to process
     * @return an immutable list of update actions which will e.g. add an interfaceInteraction to the payment
     * or apply changes to a corresponding transaction in the payment
     */
    protected abstract ImmutableList<UpdateAction<Payment>> createPaymentUpdates(final Payment payment,
                                                                                 final Notification notification);
}
