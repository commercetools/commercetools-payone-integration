package com.commercetools.pspadapter.payone.domain.payone.model.creditcard;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.CaptureRequest;

/**
 * @author fhaertig
 * @date 15.12.15
 */
public class CreditCardCaptureRequest extends CaptureRequest {

    //nothing to add here for now

    public CreditCardCaptureRequest(final PayoneConfig config) {
        super(config);
    }
}
