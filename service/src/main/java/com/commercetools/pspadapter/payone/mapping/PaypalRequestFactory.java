package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.wallet.WalletAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.wallet.WalletPreauthorizationRequest;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.google.common.base.Preconditions;
import io.sphere.sdk.payments.Payment;

import javax.annotation.Nonnull;

/**
 * @author fhaertig
 * @since 20.01.16
 */
public class PaypalRequestFactory extends PayoneRequestFactory {

    public PaypalRequestFactory(@Nonnull final TenantConfig tenantConfig) {
        super(tenantConfig);
    }

    @Override
    public WalletPreauthorizationRequest createPreauthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {

        final Payment ctPayment = paymentWithCartLike.getPayment();

        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String clearingSubType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod()).getSubType();
        final WalletPreauthorizationRequest request = new WalletPreauthorizationRequest(getPayoneConfig(), clearingSubType);

        mapFormPaymentWithCartLike(request, paymentWithCartLike);

        return request;
    }

    @Override
    @Nonnull
    public WalletAuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {

        final Payment ctPayment = paymentWithCartLike.getPayment();

        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String clearingSubType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod()).getSubType();
        WalletAuthorizationRequest request = new WalletAuthorizationRequest(getPayoneConfig(), clearingSubType);

        mapFormPaymentWithCartLike(request, paymentWithCartLike);

        return request;
    }
}
