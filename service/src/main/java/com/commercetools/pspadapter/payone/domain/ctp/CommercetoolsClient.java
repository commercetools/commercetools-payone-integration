package com.commercetools.pspadapter.payone.domain.ctp;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.commands.Command;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.Query;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class CommercetoolsClient {

    private SphereClient sphereClient;

    public CommercetoolsClient(final SphereClient client) {
        this.sphereClient = client;
    }

    public <T> CompletionStage<T> execute(SphereRequest<T> sphereRequest) {
        final CompletionStage<T> result;
        final CompletionStage<T> intermediateResult = sphereClient.execute(sphereRequest);
        if (sphereRequest instanceof Query) {
            final Function<Throwable, T> provideEmptyResultOnException = exception -> (T) PagedQueryResult.empty();
            result = intermediateResult.exceptionally(provideEmptyResultOnException);
        } else if (sphereRequest instanceof Command) {
            final Function<Throwable, T> retry = exception -> (T) sphereClient.execute(sphereRequest);
            result = intermediateResult.exceptionally(retry);
        } else {
            result = intermediateResult;
        }
        return result;
    }

}
