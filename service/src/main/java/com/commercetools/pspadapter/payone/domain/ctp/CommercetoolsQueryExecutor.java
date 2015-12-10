package com.commercetools.pspadapter.payone.domain.ctp;

import io.sphere.sdk.messages.Message;
import io.sphere.sdk.messages.queries.MessageQuery;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.queries.OrderByIdGet;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.messages.PaymentCreatedMessage;
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.Query;
import io.sphere.sdk.queries.QueryPredicate;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class CommercetoolsQueryExecutor {

    private CommercetoolsClient client;

    public CommercetoolsQueryExecutor(final CommercetoolsClient client) {
        this.client = client;
    }

    public Collection<Payment> getPaymentsWithTransactionState(TransactionState transactionState) {
        QueryPredicate<Payment> predicateFunction = QueryPredicate.of("transactions(state = \"" + transactionState.toSphereName() +"\")");
        PaymentQuery query = PaymentQuery.of().withPredicates(Arrays.asList(predicateFunction));

        return client.complete(query).getResults();
    }

    public Collection<PaymentCreatedMessage> getPaymentCreatedMessages(ZonedDateTime sinceDate, TransactionState transactionState) {
        QueryPredicate<Message> predCreatedAt = QueryPredicate.of("createdAt >= \"" + sinceDate.toLocalDateTime().toString() + "\"");
        QueryPredicate<Message> predTransactionState = QueryPredicate.of("transactions(state = \"" + transactionState.toSphereName() +"\")");
        Query<PaymentCreatedMessage> query = MessageQuery.of()
                .withPredicates(Arrays.asList(predCreatedAt, predTransactionState))
                .forMessageType(PaymentCreatedMessage.MESSAGE_HINT);

        return client.complete(query).getResults();
    }

    public Order getOrderById(String id) {
        return client.complete(OrderByIdGet.of(id));
    }
}
