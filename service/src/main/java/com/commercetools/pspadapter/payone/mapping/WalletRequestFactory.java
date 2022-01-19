package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.wallet.WalletPayoneRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.wallet.WalletPreauthorizationRequest;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.util.function.TriFunction;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentMethodInfo;

import javax.annotation.Nonnull;

/**
 * Requests factory for Wallet based payments, like <i>PayPal</i> and <i>Paydirekt</i>.
 * <p>Based on {@link PaymentMethodInfo#getMethod() Payment#paymentMethodInfo#method} value the request will be created
 * with respective {@link WalletPayoneRequest#clearingtype}, {@link WalletPayoneRequest#wallettype}
 * and {@link WalletPayoneRequest#noShipping}</p>
 */
public class WalletRequestFactory extends PayoneRequestFactory {

    public WalletRequestFactory(@Nonnull final TenantConfig tenantConfig) {
        super(tenantConfig);
    }

    @Override
    @Nonnull
    public WalletPreauthorizationRequest createPreauthorizationRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(paymentWithCartLike, WalletPreauthorizationRequest::new);
    }

    @Override
    @Nonnull
    public WalletPayoneRequest createAuthorizationRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(paymentWithCartLike, WalletPayoneRequest::new);
    }

    @Nonnull
    private <WR extends PayoneRequest> WR createRequestInternal(@Nonnull final PaymentWithCartLike paymentWithCartLike,
                                                                @Nonnull final TriFunction<? super PayoneConfig, ClearingType, Integer, WR> requestConstructor) {

        final Payment ctPayment = paymentWithCartLike.getPayment();
        final CartLike ctCartLike = paymentWithCartLike.getCartLike();

        if(ctPayment.getCustom() == null) {
            throw new IllegalArgumentException("Missing custom fields on payment!");
        }

        final int noShippingAddress = MappingUtil.checkForMissingShippingAddress(ctCartLike.getShippingAddress());
        final ClearingType clearingType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod());
        WR request = requestConstructor.apply(getPayoneConfig(), clearingType, noShippingAddress);

        mapFormPaymentWithCartLike(request, paymentWithCartLike, false);

        return request;
    }
}
