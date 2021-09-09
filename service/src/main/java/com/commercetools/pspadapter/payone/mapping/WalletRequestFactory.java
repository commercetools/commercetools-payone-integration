package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.wallet.WalletAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.wallet.WalletPreauthorizationRequest;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.util.function.TriFunction;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentMethodInfo;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;

/**
 * Requests factory for Wallet based payments, like <i>PayPal</i> and <i>Paydirekt</i>.
 * <p>Based on {@link PaymentMethodInfo#getMethod() Payment#paymentMethodInfo#method} value the request will be created
 * with respective {@link WalletAuthorizationRequest#clearingtype}, {@link WalletAuthorizationRequest#wallettype}
 * and {@link WalletAuthorizationRequest#noShipping}</p>
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
    public WalletAuthorizationRequest createAuthorizationRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(paymentWithCartLike, WalletAuthorizationRequest::new);
    }

    @Nonnull
    private <WR extends AuthorizationRequest> WR createRequestInternal(@Nonnull final PaymentWithCartLike paymentWithCartLike,
                                                                       @Nonnull final TriFunction<? super PayoneConfig, ClearingType, Integer, WR> requestConstructor) {

        final Payment ctPayment = paymentWithCartLike.getPayment();
        final CartLike ctCartLike = paymentWithCartLike.getCartLike();

        if(ctPayment.getCustom() == null) {
            throw new IllegalArgumentException("Missing custom fields on payment!");
        }

        final int noShippingAddress = MappingUtil.checkForMissingShippingAddress(ctCartLike.getShippingAddress());
        final ClearingType clearingType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod());
        WR request = requestConstructor.apply(getPayoneConfig(), clearingType, noShippingAddress);

        mapFormPaymentWithCartLike(request, paymentWithCartLike);

        return request;
    }
}
