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

    public WalletPreauthorizationRequest(final PayoneConfig config, final ClearingType clearingType) {
        super(config, RequestType.PREAUTHORIZATION.getType(), clearingType.getPayoneCode());

        this.wallettype = clearingType.getSubType();
    }

    //**************************************************************
    //* GETTER AND SETTER METHODS
    //**************************************************************


    public String getWallettype() {
        return wallettype;
    }
}
