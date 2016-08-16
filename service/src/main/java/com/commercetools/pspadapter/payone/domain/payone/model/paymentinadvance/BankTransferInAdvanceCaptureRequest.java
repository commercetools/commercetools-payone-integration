package com.commercetools.pspadapter.payone.domain.payone.model.paymentinadvance;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.CaptureRequest;

public class BankTransferInAdvanceCaptureRequest extends CaptureRequest{

    public  BankTransferInAdvanceCaptureRequest(final PayoneConfig config) {
        super(config);
    }
}
