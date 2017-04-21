package com.commercetools.pspadapter.payone.util;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public final class CompletionUtil {

    /**
     * This method is used as an intermediate step of re-factoring blocking Sphere client usage to use stages,
     * but in some places blocking operation are still required.
     * <p>
     * <b>Note:</b> usage of this method should be avoided as much as possible! At least never use a sequence of
     * {@code executeBlocking(CompletionStage<T>)}, like:
     * <pre>
     *  Payment payment = executeBlocking(paymentStage);
     *  Order order     = executeBlocking(orderService.getOrderForPayment(payment));
     * </pre>
     * Instead use {@link CompletionStage#thenComposeAsync(Function)}:
     * <pre>
     *  Order order = executeBlocking(paymentStage
     *                .thenComposeAsync(payment -> orderService.getOrderForPayment(payment)));
     * </pre>
     *
     * @param completionStage {@link CompletionStage} to wait blocking.
     * @param <T>
     * @return typed result of the finished {@code completionStage}
     */
    public static <T> T executeBlocking(CompletionStage<T> completionStage) {
        return completionStage.toCompletableFuture().join();
    }

    private CompletionUtil() {
    }
}
