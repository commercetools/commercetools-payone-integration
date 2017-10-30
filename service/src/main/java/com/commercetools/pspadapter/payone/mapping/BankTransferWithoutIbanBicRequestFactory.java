package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.google.common.base.Preconditions;
import io.sphere.sdk.payments.Payment;

import javax.annotation.Nonnull;

/**
 * Created by mht on 15.08.16.
 */
public class BankTransferWithoutIbanBicRequestFactory extends PayoneRequestFactory {
    public BankTransferWithoutIbanBicRequestFactory(@Nonnull final TenantConfig tenantConfig) {
        super(tenantConfig);
    }

    @Override
    public BankTransferAuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {

        final Payment ctPayment = paymentWithCartLike.getPayment();

        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String clearingSubType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod()).getSubType();
        BankTransferAuthorizationRequest request = new BankTransferAuthorizationRequest(getPayoneConfig(), clearingSubType);

        mapFormPaymentWithCartLike(request, paymentWithCartLike);

        //Despite declared as optional in PayOne Server API documentation. the Bankcountry is required
        request.setBankcountry(request.getCountry());
        return request;
    }
}
