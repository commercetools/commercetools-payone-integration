package com.commercetools.pspadapter.payone.domain.payone.model.banktransfer;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.util.ClearSecuredValuesSerializer;

import javax.annotation.Nonnull;

/**
 * Common bank-transfer specific Payone requests.
 *
 * @author fhaertig
 * @since 22.01.16
 */
public class BankTransferRequest extends AuthorizationRequest {

    private String onlinebanktransfertype;

    private String bankcountry;

    private String bankGroupType;

    @ClearSecuredValuesSerializer.Apply(true)
    private String iban;

    @ClearSecuredValuesSerializer.Apply(true)
    private String bic;

    /**
     * Create basic bank-transfer specific request.
     *
     * @param requestType            now only {@link RequestType#PREAUTHORIZATION} and
     *                               {@link RequestType#AUTHORIZATION} supported.
     * @param config                 Payone config to use
     * @param onlinebanktransfertype {@code onlinebanktransfertype} of the Payone bank-transfer request
     */
    public BankTransferRequest(@Nonnull final RequestType requestType,
                               @Nonnull final PayoneConfig config,
                               @Nonnull final String onlinebanktransfertype) {
        super(config, requestType.getType(), ClearingType.PAYONE_PNT.getPayoneCode());

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

    public String getBankcountry() {
        return bankcountry;
    }

    public void setBankcountry(String bankcountry) {
        this.bankcountry = bankcountry;
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

    public String getBankGroupType() {
        return bankGroupType;
    }

    public void setBankGroupType(String bankGroupType) {
        this.bankGroupType = bankGroupType;
    }
}
