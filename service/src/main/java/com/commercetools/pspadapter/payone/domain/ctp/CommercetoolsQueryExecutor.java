package com.commercetools.pspadapter.payone.domain.ctp;

import com.commercetools.pspadapter.payone.domain.ctp.exceptions.NoCartLikeFoundException;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.queries.CartQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.messages.GenericMessageImpl;
import io.sphere.sdk.messages.MessageDerivateHint;
import io.sphere.sdk.messages.expansion.MessageExpansionModel;
import io.sphere.sdk.messages.queries.MessageQuery;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.queries.OrderQuery;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.expansion.PaymentExpansionModel;
import io.sphere.sdk.payments.messages.PaymentCreatedMessage;
import io.sphere.sdk.payments.messages.PaymentTransactionAddedMessage;
import io.sphere.sdk.payments.queries.PaymentByIdGet;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.Query;

import java.time.ZonedDateTime;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * @author fhaertig
 * @since 02.12.15
 */
//TODO: refactor class since it has mixed concerns (maybe MessageConsumer only)
public class CommercetoolsQueryExecutor {

    private BlockingSphereClient client;

    public CommercetoolsQueryExecutor(final BlockingSphereClient client) {
        this.client = client;
    }

    public PaymentWithCartLike getPaymentWithCartLike(final String paymentId) {
        final CompletionStage<Payment> paymentStage = client.execute(PaymentByIdGet.of(paymentId)
                // customer is used to parse some properties,
                // see com.commercetools.pspadapter.payone.mapping.MappingUtil#mapCustomerToRequest()
                .plusExpansionPaths(PaymentExpansionModel::customer));

        return getPaymentWithCartLike(paymentId, paymentStage);
    }

    public PaymentWithCartLike getPaymentWithCartLike(String paymentId, CompletionStage<Payment> paymentFuture)  {
        final CompletionStage<PagedQueryResult<Order>> orderFuture =
                client.execute(OrderQuery.of().withPredicates(m -> m.paymentInfo().payments().id().is(paymentId)));

        final CompletionStage<PagedQueryResult<Cart>> cartFuture =
                client.execute(CartQuery.of().withPredicates(m -> m.paymentInfo().payments().id().is(paymentId)));

        final CompletionStage<PaymentWithCartLike> paymentWithCartLikeFuture = paymentFuture.thenCompose(payment ->
            orderFuture.thenCompose(orderResult -> {
                if (orderResult.getTotal() > 0) {
                    final Order order = orderResult.getResults().get(0);
                    return CompletableFuture.completedFuture(new PaymentWithCartLike(payment, order));
                } else {
                    return cartFuture.thenApply(cartResult -> {
                        if (cartResult.getTotal() > 0) {
                            return new PaymentWithCartLike(payment, cartResult.getResults().get(0));
                        } else {
                            throw new NoCartLikeFoundException();
                        }
                    });
                }
            }
        ));

        //TODO: refactor since BlockingClient is available
        try {
            return paymentWithCartLikeFuture
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            final Throwable cause =
                e.getCause() != null && e instanceof ExecutionException
                    ? e.getCause()
                    : e;
            throw cause instanceof RuntimeException? (RuntimeException) cause : new CompletionException(cause);
        }
    }

    public void consumePaymentCreatedMessages(final ZonedDateTime sinceDate, final Consumer<Payment> paymentConsumer) {
        consumeAllMessages(sinceDate, paymentConsumer, PaymentCreatedMessage.MESSAGE_HINT);
    }

    public void consumePaymentTransactionAddedMessages(final ZonedDateTime sinceDate,
                                                       final Consumer<Payment> paymentConsumer) {
        consumeAllMessages(sinceDate, paymentConsumer, PaymentTransactionAddedMessage.MESSAGE_HINT);
    }

    private <T extends GenericMessageImpl<Payment>> void consumeAllMessages(final ZonedDateTime sinceDate,
                                                                            final Consumer<Payment> paymentConsumer,
                                                                            final MessageDerivateHint<T> messageHint) {
        final MessageQuery baseQuery = MessageQuery.of()
            .withPredicates(m -> m.createdAt().isGreaterThanOrEqualTo(sinceDate))
            .withSort(m -> m.createdAt().sort().asc())
            .withExpansionPaths(MessageExpansionModel::resource)
            .withLimit(500); // Maximum

        long processed = 0;
        long total = 0;

        do {
            Query<T> query = baseQuery
                .withOffset(processed)
                .forMessageType(messageHint);
            final PagedQueryResult<T> result = client.executeBlocking(query);

            result.getResults()
                    .stream()
                    .filter(msg -> msg.getResource().getObj() != null)
                    .forEach(msg -> paymentConsumer.accept(msg.getResource().getObj()));

            processed = result.getOffset() + result.getCount();
            total = result.getTotal();
        } while (processed < total);
    }

}
