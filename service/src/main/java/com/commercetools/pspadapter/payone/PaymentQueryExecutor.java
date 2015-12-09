package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.queries.OrderByIdGet;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class PaymentQueryExecutor {

    private CommercetoolsClient client;

    public PaymentQueryExecutor(final CommercetoolsClient client) {
        this.client = client;
    }

    public Collection<Payment> getPaymentsWithTransactionState(TransactionState transactionState) {
        QueryPredicate<Payment> predicateFunction = QueryPredicate.of("transactions(state = \"" + transactionState.toSphereName() +"\")");
        PaymentQuery query = PaymentQuery.of().withPredicates(Arrays.asList(predicateFunction));

        try {
            PagedQueryResult<Payment> queryResult = client.execute(query).toCompletableFuture().get();
            return queryResult.getResults();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Order getOrderById(String id) {
        OrderByIdGet query = OrderByIdGet.of(id);

        try {
            Order queryResult = client.execute(query).toCompletableFuture().get();
            return queryResult;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
}
