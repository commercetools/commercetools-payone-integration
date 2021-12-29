package com.commercetools.pspadapter.payone.transaction.paymentinadvance;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneRequest;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;

import static io.sphere.sdk.payments.TransactionType.AUTHORIZATION;

public class BankTransferInAdvanceAuthorizationTransactionExecutor extends BaseBankTransferInAdvanceTransactionExecutor {

    public BankTransferInAdvanceAuthorizationTransactionExecutor(@Nonnull LoadingCache<String, Type> typeCache,
                                                                 @Nonnull PayoneRequestFactory requestFactory,
                                                                 @Nonnull PayonePostService payonePostService,
                                                                 @Nonnull BlockingSphereClient client) {
        super(AUTHORIZATION, typeCache, requestFactory, payonePostService, client);
    }

    @Nonnull
    @Override
    protected PayoneRequest createRequest(@Nonnull PaymentWithCartLike paymentWithCartLike) {
        return requestFactory.createPreauthorizationRequest(paymentWithCartLike);
    }
}
