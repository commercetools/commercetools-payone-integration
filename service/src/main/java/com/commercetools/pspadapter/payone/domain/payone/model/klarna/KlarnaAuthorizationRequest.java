package com.commercetools.pspadapter.payone.domain.payone.model.klarna;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ExtendedAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;

/**
 * <b>Caveat:</b> according to Payone discussion this request should not be used by the shops, because in this case
 * <i>capture</i> state is activated automatically.
 * Preferable way is to make <i>{@link KlarnaPreauthorizationRequest preauthorization}</i> request and then
 * make direct <i>capture</i>, when the goods are packaged, shipped or delivered.
 */
public class KlarnaAuthorizationRequest extends ExtendedAuthorizationRequest {

    public KlarnaAuthorizationRequest(final PayoneConfig config, final String financingtype,
                                      final PaymentWithCartLike paymentWithCartLike) {
        super(config, RequestType.AUTHORIZATION.getType(), financingtype, ClearingType.PAYONE_KLV.getPayoneCode(), paymentWithCartLike);
    }

}
