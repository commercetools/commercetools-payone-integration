package com.commercetools.pspadapter.payone.domain.payone.model.banktransfer;

import com.commercetools.pspadapter.payone.config.PayoneConfig;

import static com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType.PREAUTHORIZATION;

public class BankTransferPreathorizationRequest extends BaseBankTransferAuthorizationRequest {

    public BankTransferPreathorizationRequest(final PayoneConfig config, final String onlinebanktransfertype) {
        super(config, PREAUTHORIZATION, onlinebanktransfertype);
    }
}
