package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
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
 * @date 17.12.15
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

    public boolean dispatchNotification(final Notification notification) throws IllegalArgumentException {

        if (hasValidSecrets(notification)) {
            Payment payment = getPaymentByInterfaceId(notification.getTxid())
                    .orElseGet(() -> {
                        PaymentDraft paymentDraft = createNewPaymentDraftFromNotification(notification);
                        return client.complete(PaymentCreateCommand.of(paymentDraft));
                    });

            return processors.getOrDefault(notification.getTxaction(), defaultProcessor)
                    .processTransactionStatusNotification(notification, payment);
        } else {
            return false;
        }
    }

    /**
     * checks if the secrets of the received notification
     * are matching the corresponding config values of this service instance.
     *
     * @param notification the notification object to check
     * @return true if all secrets match
     */
    private boolean hasValidSecrets(final Notification notification) {
         return config.getKeyAsMd5Hash().equals(notification.getKey())
                && config.getPortalId().equals(notification.getPortalid())
                && config.getSubAccountId().equals(notification.getAid())
                && config.getMode().equals(notification.getMode());

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
        PaymentDraft paymentDraft = PaymentDraftBuilder
                .of(amount)
                .interfaceId(notification.getTxid())
                .build();
        return paymentDraft;
    }
}
