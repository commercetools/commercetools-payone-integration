package com.commercetools.pspadapter.payone.domain.payone.model.banktransfer;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;

/**
 * @author fhaertig
 * @since 22.01.16
 */
public class BankTransferAuthorizationRequest extends AuthorizationRequest {

    private String onlinebanktransfertype;

    private String iban;

    private String bic;

    public BankTransferAuthorizationRequest(final PayoneConfig config, final String onlinebanktransfertype) {
        super(config, RequestType.AUTHORIZATION.getType(), ClearingType.PAYONE_PNT.getPayoneCode());

        this.onlinebanktransfertype = onlinebanktransfertype;
    }

    //**************************************************************
    //* GETTER AND SETTER METHODS
    //**************************************************************


    public String getOnlinebanktransfertype() {
        return onlinebanktransfertype;
    }

    public void setOnlinebanktransfertype(final String onlinebanktransfertype) {
        this.onlinebanktransfertype = onlinebanktransfertype;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(final String iban) {
        this.iban = iban;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(final String bic) {
        this.bic = bic;
    }
}
