package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.pspadapter.payone.ServiceFactory;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.notification.NotificationProcessorBase;

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
     * @param serviceFactory the services factory for commercetools platform API
     */
    public DefaultNotificationProcessor(ServiceFactory serviceFactory) {
        super(serviceFactory);
    }

        @Override
    protected boolean canProcess(final Notification notification) {
        return true;
    }
}
