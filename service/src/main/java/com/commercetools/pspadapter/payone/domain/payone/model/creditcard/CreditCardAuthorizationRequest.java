package com.commercetools.pspadapter.payone.domain.payone.model.creditcard;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;

/**
 * @author fhaertig
 * @since 18.01.16
 */
public class CreditCardAuthorizationRequest extends AuthorizationRequest {

    private String pseudocardpan;

    public CreditCardAuthorizationRequest(final PayoneConfig config, final String pseudocardpan) {
        super(config, RequestType.AUTHORIZATION.getType(), ClearingType.PAYONE_CC.getPayoneCode());
        this.pseudocardpan = pseudocardpan;
    }

    public String getPseudocardpan() {
        return pseudocardpan;
    }
}
