package com.commercetools.pspadapter.payone.domain.payone.model.wallet;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;

/**
 * @author fhaertig
 * @since 20.01.16
 */
public class WalletPreauthorizationRequest extends PayoneRequest {

    private String wallettype;

    private int noShipping;

    public WalletPreauthorizationRequest(final PayoneConfig config, final ClearingType clearingType, final int noShipping) {
        super(config, RequestType.PREAUTHORIZATION.getType(), clearingType.getPayoneCode());

        this.wallettype = clearingType.getSubType();
        this.noShipping = noShipping;
    }

    //**************************************************************
    //* GETTER AND SETTER METHODS
    //**************************************************************


    public String getWallettype() {
        return wallettype;
    }

    public int getNoShipping() {
        return noShipping;
    }

}
