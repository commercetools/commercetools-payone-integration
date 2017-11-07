package com.commercetools.pspadapter.payone.notification;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.tenant.TenantFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.PaymentMethodInfoBuilder;
import io.sphere.sdk.utils.MoneyImpl;
import org.slf4j.LoggerFactory;

import javax.money.MonetaryAmount;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.pspadapter.payone.util.CompletionUtil.executeBlocking;

/**
 * @author fhaertig
 * @since 17.12.15
 */
public class NotificationDispatcher {

    private final NotificationProcessor defaultProcessor;
    private final ImmutableMap<NotificationAction, NotificationProcessor> processors;
    private final TenantFactory tenantFactory;
    private final PayoneConfig config;

    public NotificationDispatcher(
            final NotificationProcessor defaultProcessor,
            final ImmutableMap<NotificationAction, NotificationProcessor> processors,
            final TenantFactory tenantFactory,
            final PayoneConfig config) {
        this.defaultProcessor = defaultProcessor;
        this.processors = processors;
        this.tenantFactory = tenantFactory;
        this.config = config;
    }

    /**
     * Dispatches the {@code notification} to a notification processor
     *
     * @param notification a PAYONE transaction status notification
     * @throws ConcurrentModificationException in case the respective payment could not be updated due to concurrent
     *                                         modifications; a retry at a later time might be successful
     * @throws RuntimeException                in case of an unexpected (and probably unrecoverable) error
     */
    public void dispatchNotification(final Notification notification) {
        validateSecrets(notification);

        final NotificationProcessor notificationProcessor = getNotificationProcessor(notification.getTxaction());

        try {
            dispatchNotificationToProcessor(notification, notificationProcessor);
        } catch (final ConcurrentModificationException e) {
            LoggerFactory.getLogger(this.getClass()).warn("ConcurrentModificationException on notification [{}]. Retry once more.",
                    notification.toString());

            // try once more
            dispatchNotificationToProcessor(notification, notificationProcessor);
        }
    }

    /**
     * Checks if the secrets of the received notification
     * are matching the corresponding config values of this service instance.
     *
     * @param notification the notification object to check
     * @throws IllegalArgumentException if any argument is null or not matching
     */
    private void validateSecrets(final Notification notification) throws IllegalArgumentException {
        Preconditions.checkArgument(config.getKeyAsHash().equals(notification.getKey()),
                "the value for 'key' is not valid for this service instance: " + notification.getKey());
        Preconditions.checkArgument(config.getPortalId().equals(notification.getPortalid()),
                "the value for 'portalid' is not valid for this service instance: " + notification.getPortalid());
        Preconditions.checkArgument(config.getSubAccountId().equals(notification.getAid()),
                "the value for 'aid' is not valid for this service instance: " + notification.getAid());
        Preconditions.checkArgument(config.getMode().equals(notification.getMode()),
                "the value for 'mode' is not valid for this service instance: " + notification.getMode());
    }

    private NotificationProcessor getNotificationProcessor(final NotificationAction txAction) {
        return processors.getOrDefault(txAction, defaultProcessor);
    }

    private void dispatchNotificationToProcessor(final Notification notification,
                                                 final NotificationProcessor notificationProcessor) {

        Payment payment = executeBlocking(tenantFactory.getPaymentService()
                    .getByPaymentMethodAndInterfaceId(tenantFactory.getPayoneInterfaceName(), notification.getTxid())
                .thenComposeAsync(optionalPayment -> optionalPayment
                        .map(CompletableFuture::completedFuture)
                        .orElseGet(() -> {
                    PaymentDraft paymentDraft = createNewPaymentDraftFromNotification(notification);
                    return tenantFactory.getPaymentService().createPayment(paymentDraft).toCompletableFuture();
                })));

        notificationProcessor.processTransactionStatusNotification(notification, payment);
    }

    private PaymentDraft createNewPaymentDraftFromNotification(final Notification notification) {
        MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        ClearingType clearingType = ClearingType.getClearingTypeByCode(notification.getClearingtype());
        return PaymentDraftBuilder
                .of(amount)
                .interfaceId(notification.getTxid())
                .paymentMethodInfo(
                        PaymentMethodInfoBuilder
                                .of()
                                .paymentInterface(tenantFactory.getPayoneInterfaceName())
                                .method(clearingType.getKey())
                                .build())
                .build();
    }
}
