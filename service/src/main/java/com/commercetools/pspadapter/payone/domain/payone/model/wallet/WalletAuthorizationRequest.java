package com.commercetools.pspadapter.payone.domain.payone.model.wallet;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;

/**
 * @author fhaertig
 * @since 22.01.16
 */
public class WalletAuthorizationRequest extends AuthorizationRequest {

    private String wallettype;

    public WalletAuthorizationRequest(final PayoneConfig config, final String wallettype) {
        super(config, RequestType.AUTHORIZATION.getType(), ClearingType.PAYONE_PPE.getPayoneCode());

        this.wallettype = wallettype;
    }

    //**************************************************************
    //* GETTER AND SETTER METHODS
    //**************************************************************


    public String getWallettype() {
        return wallettype;
    }
}
