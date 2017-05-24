package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.wallet.WalletAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.wallet.WalletPreauthorizationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * @author fhaertig
 * @since 20.01.16
 */
public class PaypalRequestFactory extends PayoneRequestFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PaypalRequestFactory.class);

    public PaypalRequestFactory(final PayoneConfig config) {
        super(config);
    }

    @Override
    public WalletPreauthorizationRequest createPreauthorizationRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return createBasicAuthorizationRequest(paymentWithCartLike, WalletPreauthorizationRequest::new, LOG);
    }

    @Override
    public WalletAuthorizationRequest createAuthorizationRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return createBasicAuthorizationRequest(paymentWithCartLike, WalletAuthorizationRequest::new, LOG);
    }
}
