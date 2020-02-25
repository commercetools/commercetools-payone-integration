package com.commercetools.pspadapter.payone.domain.ctp;

import com.google.common.cache.CacheLoader;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.queries.TypeQuery;

import javax.annotation.Nonnull;

public class TypeCacheLoader extends CacheLoader<String, Type> {
    private final BlockingSphereClient client;

    public TypeCacheLoader(BlockingSphereClient client) {
        this.client = client;
    }

    @Override
    public Type load(@Nonnull final String typeKey) {
        final PagedQueryResult<Type> result = client.executeBlocking(
            TypeQuery.of()
                    .withPredicates(m -> m.key().is(typeKey))
                    .withLimit(1));
        return result.head().orElseThrow(() -> new IllegalStateException(typeKey + " was not found"));
    }
}
