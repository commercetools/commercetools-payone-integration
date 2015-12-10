package com.commercetools.pspadapter.payone.domain.ctp;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;

import java.util.concurrent.CompletionStage;

// TODO: Remove once a blocking client is part of JVM SDK.
public interface BlockingClient extends SphereClient {
    @Override
    void close();

    @Override
    <T> CompletionStage<T> execute(final SphereRequest<T> sphereRequest);

    <T> T complete(final SphereRequest<T> sphereRequest);
}
