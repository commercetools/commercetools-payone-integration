package com.commercetools.service;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.orders.commands.OrderUpdateCommand;
import io.sphere.sdk.orders.commands.updateactions.ChangePaymentState;
import io.sphere.sdk.orders.queries.OrderQuery;
import io.sphere.sdk.orders.queries.OrderQueryBuilder;
import io.sphere.sdk.queries.PagedQueryResult;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class OrderServiceImpl implements OrderService {

    private final SphereClient client;

    public OrderServiceImpl(SphereClient sphereClient) {
        this.client = sphereClient;
    }

    @Override
    public CompletionStage<Optional<Order>> getOrderByPaymentId(String paymentId) {
        OrderQuery orderWithPaymentId = OrderQueryBuilder.of()
                .predicates(order -> order.paymentInfo().payments().id().is(paymentId)).build();
        return client.execute(orderWithPaymentId).thenApplyAsync(PagedQueryResult::head);
    }

    @Override
    public CompletionStage<Order> updateOrderPaymentState(Order order, PaymentState newPaymentState) {
        return client.execute(OrderUpdateCommand.of(order, ChangePaymentState.of(newPaymentState)));
    }
}
