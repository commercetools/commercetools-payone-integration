package com.commercetools.pspadapter.payone.util;

import java.util.concurrent.CompletionStage;

public final class CompletionUtil {

    /**
     * This method us used as an intermediate step of re-factoring blocking Sphere client usage to use stages,
     * but in some places blocking operation are still required.
     * <p>
     * <b>Note:</b> usage of this method should be avoided as much as possible!
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
