package com.commercetools.pspadapter.payone.transaction.paymentinadvance;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;

import static io.sphere.sdk.payments.TransactionType.CHARGE;

/**
 * <b>Note:</b>: according to <i>TECHNICAL REFERENCE PAYONE Platform Channel Server API v2.92</i>
 * (chapter <i>3.2.2 Initiating payment process (authorization)</i>)
 * and <i>Sequenzdiagramme20170130</i> (chapter <i>Retailer – Zahlart Vorkasse mit Vorautorisierung</i>)
 * this transaction type (Payone <i>authorization</i>) is <b>not</b> supported for this payment method:
 * <pre>
 *     Prepayment   Not supported by this request!
 * </pre>
 * <p>
 * Looks like it is accepted by Payone service, but might be switched off in the future any time.
 */
public class BankTransferInAdvanceChargeTransactionExecutor extends BaseBankTransferInAdvanceTransactionExecutor {

    public BankTransferInAdvanceChargeTransactionExecutor(@Nonnull LoadingCache<String, Type> typeCache,
                                                          @Nonnull PayoneRequestFactory requestFactory,
                                                          @Nonnull PayonePostService payonePostService,
                                                          @Nonnull BlockingSphereClient client) {
        super(CHARGE, typeCache, requestFactory, payonePostService, client);
    }

    @Nonnull
    @Override
    protected AuthorizationRequest createRequest(@Nonnull PaymentWithCartLike paymentWithCartLike) {
        return requestFactory.createAuthorizationRequest(paymentWithCartLike);
    }
}
