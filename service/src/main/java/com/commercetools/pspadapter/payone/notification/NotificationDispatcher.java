package com.commercetools.pspadapter.payone.notification;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.utils.MoneyImpl;

import javax.money.MonetaryAmount;
import java.util.Optional;

/**
 * @author fhaertig
 * @since 17.12.15
 */
public class NotificationDispatcher {

    private final NotificationProcessor defaultProcessor;
    private final ImmutableMap<NotificationAction, NotificationProcessor> processors;
    private final CommercetoolsClient client;
    private final PayoneConfig config;

    public NotificationDispatcher(
            final NotificationProcessor defaultProcessor,
            final ImmutableMap<NotificationAction, NotificationProcessor> processors,
            final CommercetoolsClient client,
            final PayoneConfig config) {
        this.defaultProcessor = defaultProcessor;
        this.processors = processors;
        this.client = client;
        this.config = config;
    }

    public boolean dispatchNotification(final Notification notification) {

        validateSecrets(notification);

        Payment payment = getPaymentByInterfaceId(notification.getTxid())
                .orElseGet(() -> {
                    PaymentDraft paymentDraft = createNewPaymentDraftFromNotification(notification);
                    return client.complete(PaymentCreateCommand.of(paymentDraft));
                });

        return processors.getOrDefault(notification.getTxaction(), defaultProcessor)
                .processTransactionStatusNotification(notification, payment);
    }

    /**
     * Checks if the secrets of the received notification
     * are matching the corresponding config values of this service instance.
     *
     * @param notification the notification object to check
     * @throws IllegalArgumentException if any argument is null or not matching
     */
    private void validateSecrets(final Notification notification) throws IllegalArgumentException {
        Preconditions.checkArgument(config.getKeyAsMd5Hash().equals(notification.getKey()),
                "the value for 'key' is not valid for this service instance: " + notification.getKey());
        Preconditions.checkArgument(config.getPortalId().equals(notification.getPortalid()),
                "the value for 'portalid' is not valid for this service instance: " + notification.getPortalid());
        Preconditions.checkArgument(config.getSubAccountId().equals(notification.getAid()),
                "the value for 'aid' is not valid for this service instance: " + notification.getAid());
        Preconditions.checkArgument(config.getMode().equals(notification.getMode()),
                "the value for 'mode' is not valid for this service instance: " + notification.getMode());
    }

    private Optional<Payment> getPaymentByInterfaceId(final String interfaceId) {
        final PagedQueryResult<Payment> queryResult = client.complete(
                PaymentQuery.of()
                        .plusPredicates(p -> p.interfaceId().is(interfaceId))
                        .plusPredicates(p -> p.paymentMethodInfo().paymentInterface().is("PAYONE")));
        return queryResult.head();
    }

    private PaymentDraft createNewPaymentDraftFromNotification(final Notification notification) {
        MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        return PaymentDraftBuilder
                .of(amount)
                .interfaceId(notification.getTxid())
                .build();
    }
}
