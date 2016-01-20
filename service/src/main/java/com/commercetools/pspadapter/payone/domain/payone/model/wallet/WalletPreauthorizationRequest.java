package com.commercetools.pspadapter.payone.domain.payone.model.wallet;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;

/**
 * @author fhaertig
 * @since 20.01.16
 */
public class WalletPreauthorizationRequest extends AuthorizationRequest {

    private String wallettype;

    private String successurl;

    private String errorurl;

    private String backurl;

    public WalletPreauthorizationRequest(final PayoneConfig config, final String wallettype) {
        super(config, RequestType.PREAUTHORIZATION.getType(), ClearingType.PAYONE_PPE.getPayoneCode());

        this.wallettype = wallettype;
    }

    //**************************************************************
    //* GETTER AND SETTER METHODS
    //**************************************************************


    public String getWallettype() {
        return wallettype;
    }

    public String getSuccessurl() {
        return successurl;
    }

    public void setSuccessurl(final String successurl) {
        this.successurl = successurl;
    }

    public String getErrorurl() {
        return errorurl;
    }

    public void setErrorurl(final String errorurl) {
        this.errorurl = errorurl;
    }

    public String getBackurl() {
        return backurl;
    }

    public void setBackurl(final String backurl) {
        this.backurl = backurl;
    }
}
