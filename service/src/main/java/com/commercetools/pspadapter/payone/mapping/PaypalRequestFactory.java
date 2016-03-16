package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.wallet.WalletAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.wallet.WalletPreauthorizationRequest;
import com.google.common.base.Preconditions;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.payments.Payment;
import org.javamoney.moneta.function.MonetaryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author fhaertig
 * @date 20.01.16
 */
public class PaypalRequestFactory extends PayoneRequestFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PaypalRequestFactory.class);

    public PaypalRequestFactory(final PayoneConfig config) {
        super(config);
    }

    @Override
    public WalletPreauthorizationRequest createPreauthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {

        final Payment ctPayment = paymentWithCartLike.getPayment();
        final CartLike ctCartLike = paymentWithCartLike.getCartLike();

        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String clearingSubType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod()).getSubType();
        WalletPreauthorizationRequest request = new WalletPreauthorizationRequest(getConfig(), clearingSubType);

        request.setReference(paymentWithCartLike.getOrderNumber());

        Optional.ofNullable(ctPayment.getAmountPlanned())
                .ifPresent(amount -> {
                    request.setCurrency(amount.getCurrency().getCurrencyCode());
                    request.setAmount(MonetaryUtil
                            .minorUnits()
                            .queryFrom(amount)
                            .intValue());
                });

        MappingUtil.mapCustomFieldsFromPayment(request, ctPayment.getCustom());

        try {
            MappingUtil.mapCustomerToRequest(request, ctPayment.getCustomer());
        } catch (final IllegalArgumentException ex) {
            LOG.warn("Could not fully map payment with ID " + paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        try {
            MappingUtil.mapBillingAddressToRequest(request, ctCartLike.getBillingAddress());
        } catch (final IllegalArgumentException ex) {
            LOG.warn("Could not fully map payment with ID " + paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        try {
            MappingUtil.mapShippingAddressToRequest(request, ctCartLike.getShippingAddress());
        } catch (final IllegalArgumentException ex) {
            LOG.warn("Could not fully map payment with ID " + paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        return request;
    }

    @Override
    public WalletAuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {

        final Payment ctPayment = paymentWithCartLike.getPayment();
        final CartLike ctCartLike = paymentWithCartLike.getCartLike();

        final String clearingSubType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod()).getSubType();
        WalletAuthorizationRequest request = new WalletAuthorizationRequest(getConfig(), clearingSubType);

        request.setReference(paymentWithCartLike.getOrderNumber());

        Optional.ofNullable(ctPayment.getAmountPlanned())
                .ifPresent(amount -> {
                    request.setCurrency(amount.getCurrency().getCurrencyCode());
                    request.setAmount(MonetaryUtil
                            .minorUnits()
                            .queryFrom(amount)
                            .intValue());
                });

        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");
        MappingUtil.mapCustomFieldsFromPayment(request, ctPayment.getCustom());

        try {
            MappingUtil.mapCustomerToRequest(request, ctPayment.getCustomer());
        } catch (final IllegalArgumentException ex) {
            LOG.warn("Could not fully map payment with ID " + paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        try {
            MappingUtil.mapBillingAddressToRequest(request, ctCartLike.getBillingAddress());
        } catch (final IllegalArgumentException ex) {
            LOG.warn("Could not fully map payment with ID " + paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        try {
            MappingUtil.mapShippingAddressToRequest(request, ctCartLike.getShippingAddress());
        } catch (final IllegalArgumentException ex) {
            LOG.warn("Could not fully map payment with ID " + paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        return request;
    }
}
