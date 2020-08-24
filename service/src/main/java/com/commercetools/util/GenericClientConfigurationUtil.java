package com.commercetools.util;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereClientFactory;

import javax.annotation.Nonnull;


public class GenericClientConfigurationUtil {

    protected static SphereClient createHttpClient(@Nonnull final SphereClientConfig clientConfig) {
        return SphereClientFactory.of().createClient(clientConfig);
    }

    protected GenericClientConfigurationUtil() {}
}
