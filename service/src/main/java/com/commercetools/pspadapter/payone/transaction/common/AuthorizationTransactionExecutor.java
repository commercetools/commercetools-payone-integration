package com.commercetools.pspadapter.payone.transaction.common;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.model.common.BaseRequest;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.commercetools.pspadapter.payone.transaction.BaseDefaultTransactionExecutor;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * {@link TransactionType#AUTHORIZATION} (Payone - <i>preauthorization</i>) transaction executor implementation.
 */
public class AuthorizationTransactionExecutor extends BaseDefaultTransactionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationTransactionExecutor.class);

    public AuthorizationTransactionExecutor(@Nonnull final LoadingCache<String, Type> typeCache,
                                            @Nonnull final PayoneRequestFactory requestFactory,
                                            @Nonnull final PayonePostService payonePostService,
                                            @Nonnull final BlockingSphereClient client) {
        super(typeCache, requestFactory, payonePostService, client);
    }

    @Override
    @Nonnull
    public TransactionType supportedTransactionType() {
        return TransactionType.AUTHORIZATION;
    }

    @Override
    @Nonnull
    protected BaseRequest createRequest(@Nonnull PaymentWithCartLike paymentWithCartLike) {
        return requestFactory.createPreauthorizationRequest(paymentWithCartLike);
    }

    @Override
    @Nonnull
    protected Logger getClassLogger() {
        return LOGGER;
    }
}
