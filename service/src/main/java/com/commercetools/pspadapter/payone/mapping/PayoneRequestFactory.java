package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PreauthorizationRequest;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.payments.Payment;

/**
 * @author fhaertig
 * @date 14.12.15
 */
public abstract class PayoneRequestFactory {

    public PreauthorizationRequest createPreauthorizationRequest(final PaymentWithCartLike paymentWithCartLike, final PayoneConfig config) {
        throw new UnsupportedOperationException("this request type is not supported by this payment method.");
    }

    //further methods e.g. capture,...
}
