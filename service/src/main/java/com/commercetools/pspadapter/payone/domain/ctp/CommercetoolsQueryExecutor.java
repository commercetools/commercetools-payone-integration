package com.commercetools.pspadapter.payone.domain.ctp;

import io.sphere.sdk.messages.GenericMessageImpl;
import io.sphere.sdk.messages.MessageDerivateHint;
import io.sphere.sdk.messages.queries.MessageQuery;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.queries.OrderByIdGet;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.messages.PaymentCreatedMessage;
import io.sphere.sdk.payments.messages.PaymentTransactionAddedMessage;
import io.sphere.sdk.payments.queries.PaymentByIdGet;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.Query;

import java.time.ZonedDateTime;
import java.util.function.Consumer;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class CommercetoolsQueryExecutor {

    private CommercetoolsClient client;

    public CommercetoolsQueryExecutor(final CommercetoolsClient client) {
        this.client = client;
    }

    public Order getOrderById(String id) {
        return client.complete(OrderByIdGet.of(id));
    }

    public Payment getPaymentById(String id) {
        return client.complete(PaymentByIdGet.of(id));
    }

    public void consumePaymentCreatedMessages(ZonedDateTime sinceDate, Consumer<Payment> paymentConsumer) {
        consumeAllMessages(sinceDate, paymentConsumer, PaymentCreatedMessage.MESSAGE_HINT);
    }

    public void consumePaymentTransactionAddedMessages(ZonedDateTime sinceDate, Consumer<Payment> paymentConsumer) {
        consumeAllMessages(sinceDate, paymentConsumer, PaymentTransactionAddedMessage.MESSAGE_HINT);
    }

    private <T extends GenericMessageImpl<Payment>> void consumeAllMessages(ZonedDateTime sinceDate, Consumer<Payment> paymentConsumer, MessageDerivateHint<T> messageHint) {
        final MessageQuery baseQuery = MessageQuery.of()
            .withPredicates(m -> m.createdAt().isGreaterThanOrEqualTo(sinceDate))
            .withSort(m -> m.createdAt().sort().asc())
            .withExpansionPaths(m -> m.resource())
            .withLimit(500); // Maximum

        long processed = 0, total = 0;

        do {
            Query<T> query = baseQuery
                .withOffset(processed)
                .<T>forMessageType(messageHint);
            final PagedQueryResult<T> result = client.complete(query);

            // TODO null check
            result.getResults().forEach(msg -> paymentConsumer.accept(msg.getResource().getObj()));

            processed = result.getOffset() + result.size();
            total = result.getTotal();
        } while (processed < total);
    }
}
