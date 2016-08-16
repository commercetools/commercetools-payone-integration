package com.commercetools.pspadapter.payone.domain.payone.model.paymentinadvance;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;

/**
 * Due to the type of the CTP-Transaction we create a
 * @author mht@dotsource.de
 *
 */
public class BankTransferInAdvancePreautorizationRequest extends AuthorizationRequest {


    //CashInAdvance only allow PreAuthorization in the wording of payone
    public BankTransferInAdvancePreautorizationRequest(PayoneConfig config, String clearingtype) {
        super(config, RequestType.PREAUTHORIZATION.getType(), ClearingType.PAYONE_VOR.getPayoneCode());
    }
}
