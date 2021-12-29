package com.commercetools.pspadapter.payone.domain.payone.model.paymentinadvance;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;

import javax.annotation.Nonnull;

/**
 * @author mht@dotsource.de
 */
public class BankTransferInAdvanceRequest extends PayoneRequest {

    public BankTransferInAdvanceRequest(@Nonnull RequestType requestType,
                                        @Nonnull PayoneConfig config) {
        super(config, requestType.getType(), ClearingType.PAYONE_VOR.getPayoneCode());
    }
}
