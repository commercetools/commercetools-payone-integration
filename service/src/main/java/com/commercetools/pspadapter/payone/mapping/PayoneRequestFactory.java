package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.CaptureRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;

/**
 * @author fhaertig
 * @since 14.12.15
 */
public abstract class PayoneRequestFactory {

    private PayoneConfig config;

    public PayoneConfig getPayoneConfig() {
        return config;
    }

    public PayoneRequestFactory(final PayoneConfig config) {
        this.config = config;
    }

    public AuthorizationRequest createPreauthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        throw new UnsupportedOperationException("this request type is not supported by this payment method.");
    }

    public AuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        throw new UnsupportedOperationException("this request type is not supported by this payment method.");
    }

    public CaptureRequest createCaptureRequest(final PaymentWithCartLike paymentWithCartLike, final int sequenceNumber) {
        throw new UnsupportedOperationException("this request type is not supported by this payment method.");
    }

}
