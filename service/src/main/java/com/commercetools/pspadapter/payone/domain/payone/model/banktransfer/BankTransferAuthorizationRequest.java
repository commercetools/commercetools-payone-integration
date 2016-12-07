package com.commercetools.pspadapter.payone.domain.payone.model.banktransfer;

import com.commercetools.pspadapter.payone.config.PayoneConfig;

import static com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType.AUTHORIZATION;

public class BankTransferAuthorizationRequest extends BaseBankTransferAuthorizationRequest {

    public BankTransferAuthorizationRequest(final PayoneConfig config, final String onlinebanktransfertype) {
        super(config, AUTHORIZATION, onlinebanktransfertype);
    }
}
