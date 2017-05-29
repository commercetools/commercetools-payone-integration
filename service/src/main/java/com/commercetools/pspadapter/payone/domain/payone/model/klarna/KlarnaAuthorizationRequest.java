package com.commercetools.pspadapter.payone.domain.payone.model.klarna;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;

/**
 * TODO: clarify with Payone one more time: should we use this transaction type or not?
 * One told only "preauthorization" should be used!
 */
public class KlarnaAuthorizationRequest extends BaseKlarnaRequest {

    public KlarnaAuthorizationRequest(final PayoneConfig config, final String financingtype,
                                      final PaymentWithCartLike paymentWithCartLike) {
        super(config, RequestType.AUTHORIZATION.getType(), financingtype, paymentWithCartLike);
    }

}
