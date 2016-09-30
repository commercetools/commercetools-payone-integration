package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.notification.NotificationProcessorBase;
import com.google.common.collect.ImmutableList;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;

/**
 * A NotificationProcessor that simply adds an interface interaction of type
 * {@value CustomTypeBuilder#PAYONE_INTERACTION_NOTIFICATION} to the payment.
 *
 * @author Jan Wolter
 */
public class DefaultNotificationProcessor extends NotificationProcessorBase {

    /**
     * Initializes a new instance.
     *
     * @param client the commercetools platform client to update payments
     */
    public DefaultNotificationProcessor(final BlockingSphereClient client) {
        super(client);
    }

    @Override
    protected boolean canProcess(final Notification notification) {
        return true;
    }
}
