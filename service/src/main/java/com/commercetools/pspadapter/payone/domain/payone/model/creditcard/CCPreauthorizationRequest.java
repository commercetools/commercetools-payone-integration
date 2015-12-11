package com.commercetools.pspadapter.payone.domain.payone.model.creditcard;

import com.commercetools.pspadapter.payone.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PreauthorizationRequest;

/**
 * @author fhaertig
 * @date 11.12.15
 */
public class CCPreauthorizationRequest extends PreauthorizationRequest {

    /**
     * truncated card number
     */
    private String pseudocardpan;

    public CCPreauthorizationRequest(final PayoneConfig config) {
        super(config);
    }

    //**************************************************************
    //* GETTER AND SETTER METHODS
    //**************************************************************


    public String getPseudocardpan() {
        return pseudocardpan;
    }

    public void setPseudocardpan(final String pseudocardpan) {
        this.pseudocardpan = pseudocardpan;
    }
}
