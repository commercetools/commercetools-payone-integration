package com.commercetools.pspadapter.payone.domain.payone.model.klarna;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.StartSessionRequestWithCart;

import javax.annotation.Nullable;

public class KlarnaStartSessionRequest extends StartSessionRequestWithCart {

    public static final String GENERICPAYMENT = "genericpayment";

    public KlarnaStartSessionRequest(final PayoneConfig config,
                                     @Nullable final String financingtype,
                                     final PaymentWithCartLike paymentWithCartLike) {
        super(config, RequestType.GENERICPAYMEMT.getType(),financingtype, ClearingType.PAYONE_KIV.getPayoneCode(),
                paymentWithCartLike, null);

    }
}
