package com.commercetools.pspadapter.payone.domain.payone;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;

/**
 * @author fhaertig
 * @date 17.12.15
 */
public class NotificationDispatcher {
    public void dispatchNotification(final Notification notification) {
        switch (notification.getTxaction()) {
            case APPOINTED:
                break;
            case CAPTURE:
                break;
            case PAID:
                break;
            case UNDERPAID:
                break;
            case CANCELATION:
                break;
            case REFUND:
                break;
            case DEBIT:
                break;
            case REMINDER:
                break;
            case TRANSFER:
                break;
            case INVOICE:
                break;
        }
    }
}
