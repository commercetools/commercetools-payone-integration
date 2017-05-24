package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BaseBankTransferAuthorizationRequest;

import javax.annotation.Nonnull;

/**
 * @author mht@dotsource.de
 */
public class PostFinanceBanktransferRequestFactory extends BankTransferWithoutIbanBicRequestFactory {

    public PostFinanceBanktransferRequestFactory(PayoneConfig payoneConfig) {
        super(payoneConfig);
    }

    @Override
    public BaseBankTransferAuthorizationRequest createAuthorizationRequest(@Nonnull PaymentWithCartLike paymentWithCartLike) {
        final BaseBankTransferAuthorizationRequest request = super.createAuthorizationRequest(paymentWithCartLike);
        //Despite declared as optional in PayOne Server API documentation. this is required!
        request.setBankcountry("CH");
        return request;
    }
}
