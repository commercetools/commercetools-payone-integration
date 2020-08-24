package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.util.function.TriFunction;

import javax.annotation.Nonnull;

public class IDealRequestFactory extends BankTransferWithoutIbanBicRequestFactory {
    public IDealRequestFactory(@Nonnull TenantConfig tenantConfig) {
        super(tenantConfig);
    }

    @Nonnull
    @Override
    public BankTransferRequest createRequestInternal(@Nonnull RequestType requestType,
                                                     @Nonnull PaymentWithCartLike paymentWithCartLike,
                                                     @Nonnull TriFunction<RequestType, PayoneConfig, String, BankTransferRequest> requestConstructor) {
        BankTransferRequest requestInternal = super.createRequestInternal(requestType, paymentWithCartLike, requestConstructor);
        requestInternal.setBankGroupType(paymentWithCartLike.getPayment().getCustom().getFieldAsString(CustomFieldKeys.BANK_GROUP_TYPE));
        return requestInternal;
    }
}
