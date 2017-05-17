package com.commercetools.pspadapter.payone.mapping.klarna;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.BaseKlarnaRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaPreauthorizationRequest;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.google.common.base.Preconditions;
import io.sphere.sdk.payments.Payment;

import javax.annotation.Nonnull;

public class KlarnaRequestFactory extends PayoneRequestFactory {

    public KlarnaRequestFactory(@Nonnull final TenantConfig tenantConfig) {
        super(tenantConfig);
    }

    @Override
    public KlarnaPreauthorizationRequest createPreauthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(paymentWithCartLike, KlarnaPreauthorizationRequest::new);
    }

    @Override
    public KlarnaAuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(paymentWithCartLike, KlarnaAuthorizationRequest::new);
    }

    protected <BKR extends BaseKlarnaRequest> BKR createRequestInternal(final PaymentWithCartLike paymentWithCartLike,
                                                                        final TriFunction<PayoneConfig, String, PaymentWithCartLike, BKR> requestConstructor) {
        final Payment ctPayment = paymentWithCartLike.getPayment();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String clearingSubType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod()).getSubType();

        final BKR request = requestConstructor.apply(getPayoneConfig(), clearingSubType, paymentWithCartLike);

        mapFormPaymentWithCartLike(request, paymentWithCartLike);
        mapKlarnaMandatoryFields(request, paymentWithCartLike);

        //the line items, discounts and shipment cost are counted in the Klarna request constructor

        return request;
    }

    @FunctionalInterface
    private interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
