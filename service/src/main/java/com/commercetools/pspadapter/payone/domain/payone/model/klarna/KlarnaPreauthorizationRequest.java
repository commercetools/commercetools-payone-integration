package com.commercetools.pspadapter.payone.domain.payone.model.klarna;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneRequestWithCart;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;

public class KlarnaPreauthorizationRequest extends PayoneRequestWithCart {

    public KlarnaPreauthorizationRequest(final PayoneConfig config, final String financingtype,
                                         final PaymentWithCartLike paymentWithCartLike) {
        super(config, RequestType.PREAUTHORIZATION.getType(), financingtype, ClearingType.PAYONE_KIV.getPayoneCode(), paymentWithCartLike);
    }
}
