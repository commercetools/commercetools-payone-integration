package com.commercetools.pspadapter.payone.domain.payone.model.klarna;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.StartSessionRequestWithCart;

public class KlarnaStartSessionRequest extends StartSessionRequestWithCart {

    public KlarnaStartSessionRequest(final PayoneConfig config,
                                     final PaymentWithCartLike paymentWithCartLike) {
        super(config, RequestType.GENERICPAYMEMT.getType(), ClearingType.PAYONE_KIV.getPayoneCode(),
                paymentWithCartLike, null);
    }
}
