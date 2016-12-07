package com.commercetools.pspadapter.payone.domain.payone.model.banktransfer;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.util.ClearSecuredValuesSerializer;

import javax.annotation.Nullable;

/**
 * Base class for all bank transfer <b>authorisation/preauthorization</b> requests,
 * including postfinance, sofort and so on.
 */
public class BaseBankTransferAuthorizationRequest extends AuthorizationRequest {

    private String onlinebanktransfertype;

    private String bankcountry;

    @ClearSecuredValuesSerializer.Apply
    private String iban;

    @ClearSecuredValuesSerializer.Apply
    private String bic;

    BaseBankTransferAuthorizationRequest(final PayoneConfig config, final RequestType request,
                                                final String onlinebanktransfertype) {
        super(config, request.getType(), ClearingType.PAYONE_PNT.getPayoneCode());

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

    @Nullable
    public String getBankcountry() {
        return bankcountry;
    }

    public void setBankcountry(String bankcountry) {
        this.bankcountry = bankcountry;
    }


    @Nullable
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
