package com.commercetools.pspadapter.payone.domain.payone.model.creditcard;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneRequestWithCart;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;

/**
 * @author fhaertig
 * @since 11.12.15
 */
public class CreditCardPreauthorizationRequest extends PayoneRequestWithCart {

    /**
     * pseudo card number
     */
    private String pseudocardpan;

    private String ecommercemode;

    public CreditCardPreauthorizationRequest(final PayoneConfig config,
                                             final String pseudocardpan,
                                             final PaymentWithCartLike paymentWithCartLike) {
        super(config, RequestType.PREAUTHORIZATION.getType(), null, ClearingType.PAYONE_CC.getPayoneCode(),
                paymentWithCartLike);

        this.pseudocardpan = pseudocardpan;
    }

    //**************************************************************
    //* GETTER AND SETTER METHODS
    //**************************************************************


    public String getPseudocardpan() {
        return pseudocardpan;
    }

    public String getEcommercemode() {
        return ecommercemode;
    }

    public void setEcommercemode(final String ecommercemode) {
        this.ecommercemode = ecommercemode;
    }
}
