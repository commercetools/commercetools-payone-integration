package com.commercetools.pspadapter.payone.domain.payone.model.klarna;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;

public class KlarnaAuthorizationRequest extends BaseKlarnaRequest {

    public KlarnaAuthorizationRequest(final PayoneConfig config, final String financingtype,
                                      final PaymentWithCartLike paymentWithCartLike) {
        super(config, RequestType.AUTHORIZATION.getType(), financingtype, paymentWithCartLike);
    }

}
