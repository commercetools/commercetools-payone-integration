package com.commercetools.pspadapter.payone.domain.ctp;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;

import java.util.concurrent.*;

/**
 * @author fhaertig
 * @since 02.12.15
 */
public class CommercetoolsClient implements BlockingClient {

    private SphereClient sphereClient;

    public CommercetoolsClient(final SphereClient client) {
        this.sphereClient = client;
    }

    @Override
    public void close() {
        sphereClient.close();
    }

    public <T> CompletionStage<T> execute(SphereRequest<T> sphereRequest) {
        return sphereClient.execute(sphereRequest);
    }

    @Override
    public <T> T complete(SphereRequest<T> sphereRequest) {
        try {
            return execute(sphereRequest).toCompletableFuture().get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            final Throwable cause =
                e.getCause() != null && e instanceof ExecutionException
                    ? e.getCause()
                    : e;
            throw cause instanceof RuntimeException? (RuntimeException) cause : new CompletionException(cause);
        }
    }

}
