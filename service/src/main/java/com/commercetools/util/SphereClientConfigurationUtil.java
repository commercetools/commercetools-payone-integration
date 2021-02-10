package com.commercetools.util;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.retry.RetryableSphereClientBuilder;
import io.sphere.sdk.http.AsyncHttpClientAdapter;
import io.sphere.sdk.http.HttpClient;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public final class SphereClientConfigurationUtil {
    private static final long CTP_EXECUTE_COMPLETABLE_FUTURE_TIMEOUT = 10;
    private static final TimeUnit DEFAULT_TIMEOUT_TIME_UNIT = TimeUnit.SECONDS;
    private static final int MAX_PARALLEL_REQUESTS = 30;

    /**
     * Creates a {@link SphereClient} with a default {@code timeout} value of 60 seconds.
     *
     * @param clientConfig the client configuration for the client.
     * @return the instantiated {@link SphereClient}.
     */
    public static SphereClient createClient(@Nonnull final SphereClientConfig clientConfig) {

        final HttpClient httpClient = getHttpClient();
        return RetryableSphereClientBuilder.of(clientConfig, httpClient)
                                           .withMaxParallelRequests(MAX_PARALLEL_REQUESTS)
                                           .build();
    }

    /**
     * Creates a {@link BlockingSphereClient} with a default {@code timeout} of 10 seconds.
     *
     * @param clientConfig the client configuration for the client.
     * @return the instantiated {@link BlockingSphereClient}.
     */
    public static BlockingSphereClient createBlockingClient(@Nonnull final SphereClientConfig clientConfig) {

        return BlockingSphereClient.of(createClient(clientConfig), CTP_EXECUTE_COMPLETABLE_FUTURE_TIMEOUT,
                DEFAULT_TIMEOUT_TIME_UNIT);
    }

    /**
     * Gets an asynchronous {@link HttpClient} of `asynchttpclient` library, to be used by as an
     * underlying http client for the {@link SphereClient}.
     *
     * @return an asynchronous {@link HttpClient}
     */
    private static HttpClient getHttpClient() {
        final AsyncHttpClient asyncHttpClient =
            new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().build());
        return AsyncHttpClientAdapter.of(asyncHttpClient);
    }

    private SphereClientConfigurationUtil() {}
}
