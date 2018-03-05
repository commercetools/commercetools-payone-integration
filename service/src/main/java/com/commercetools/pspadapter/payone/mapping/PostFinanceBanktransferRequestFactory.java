package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferRequest;
import com.commercetools.pspadapter.tenant.TenantConfig;

import javax.annotation.Nonnull;

/**
 * @author mht@dotsource.de
 */
public class PostFinanceBanktransferRequestFactory extends BankTransferWithoutIbanBicRequestFactory {

    public PostFinanceBanktransferRequestFactory(@Nonnull final TenantConfig tenantConfig) {
        super(tenantConfig);
    }

    @Override
    @Nonnull
    public BankTransferRequest createAuthorizationRequest(PaymentWithCartLike paymentWithCartLike) {
        final BankTransferRequest request = super.createAuthorizationRequest(paymentWithCartLike);
        //Despite declared as optional in PayOne Server API documentation. this is required!
        request.setBankcountry("CH");
        return request;
    }
}
