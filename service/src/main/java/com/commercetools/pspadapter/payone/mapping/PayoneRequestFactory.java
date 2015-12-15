package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PreauthorizationRequest;

/**
 * @author fhaertig
 * @date 14.12.15
 */
public abstract class PayoneRequestFactory {

    private PayoneConfig config;

    public PayoneConfig getConfig() {
        return config;
    }

    public PayoneRequestFactory(final PayoneConfig config) {
        this.config = config;
    }

    public PreauthorizationRequest createPreauthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        throw new UnsupportedOperationException("this request type is not supported by this payment method.");
    }

    //further methods e.g. capture,...
}
