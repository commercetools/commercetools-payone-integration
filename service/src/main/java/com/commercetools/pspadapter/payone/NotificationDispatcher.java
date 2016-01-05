package com.commercetools.pspadapter.payone;

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

/**
 * @author fhaertig
 * @date 17.12.15
 */
public class NotificationDispatcher {

    private final NotificationProcessor defaultProcessor;
    private final ImmutableMap<NotificationAction, NotificationProcessor> processors;
    private final CommercetoolsClient client;

    public NotificationDispatcher(
            final NotificationProcessor defaultProcessor,
            final ImmutableMap<NotificationAction, NotificationProcessor> processors,
            final CommercetoolsClient client) {
        this.defaultProcessor = defaultProcessor;
        this.processors = processors;
        this.client = client;
    }

    public boolean dispatchNotification(final Notification notification) {

        Payment payment = getPaymentByInterfaceIdOrCreateNew(notification.getTxid());

        if (payment == null) {
            //matching payment could not be found, create it
            PaymentDraft paymentDraft = createNewPaymentDraftFromNotification(notification);
            payment = client.complete(PaymentCreateCommand.of(paymentDraft));
        }

        return processors.getOrDefault(notification.getTxaction(), defaultProcessor)
            .processTransactionStatusNotification(notification, payment);
    }

    private Payment getPaymentByInterfaceIdOrCreateNew(final String interfaceId) {
        final PagedQueryResult<Payment> queryResult = client.complete(
                PaymentQuery.of()
                        .withPredicates(p -> p.interfaceId().is(interfaceId))
                        .withPredicates(p -> p.paymentMethodInfo().paymentInterface().is("PAYONE")));

        if (queryResult.getTotal() > 0) {
            return queryResult.getResults().get(0);
        } else {
            return null;
        }
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
