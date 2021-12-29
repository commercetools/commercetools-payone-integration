package com.commercetools.pspadapter.payone.domain.payone.model.creditcard;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequestWithCart;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneRequestWithCart;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;

/**
 * @author fhaertig
 * @since 18.01.16
 */
public class CreditCardPayoneRequest extends AuthorizationRequestWithCart {

    private String pseudocardpan;

    public CreditCardPayoneRequest(final PayoneConfig config, final String pseudocardpan,
                                   final PaymentWithCartLike paymentWithCartLike) {
        super(config, RequestType.AUTHORIZATION.getType(), null, ClearingType.PAYONE_CC.getPayoneCode(),
                paymentWithCartLike);
        this.pseudocardpan = pseudocardpan;
    }

    public String getPseudocardpan() {
        return pseudocardpan;
    }
}
