package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
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

        WalletPreauthorizationRequest request = new WalletPreauthorizationRequest(getConfig(), "PPE");

        final Payment ctPayment = paymentWithCartLike.getPayment();
        final CartLike ctCartLike = paymentWithCartLike.getCartLike();

        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        //TODO: determine from custom object definition if not present at Order
        paymentWithCartLike.getOrderNumber().ifPresent(request::setReference);

        Optional.ofNullable(ctPayment.getAmountPlanned())
                .ifPresent(amount -> {
                    request.setCurrency(amount.getCurrency().getCurrencyCode());
                    request.setAmount(MonetaryUtil
                            .minorUnits()
                            .queryFrom(amount)
                            .intValue());
                });
        request.setNarrative_text(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.REFERENCE_TEXT_FIELD));
        request.setUserid(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.USER_ID_FIELD));

        try {
            MappingUtil.mapCustomerToRequest(request, ctPayment.getCustomer());
        } catch (final NullPointerException ex) {
            LOG.warn("Could not map customer details from payment with ID " + paymentWithCartLike.getPayment().getId(), ex);
        }

        try {
            MappingUtil.mapBillingAddressToRequest(request, ctCartLike.getBillingAddress());
        } catch (final NullPointerException ex) {
            LOG.warn("Could not map billing details from payment with ID " + paymentWithCartLike.getPayment().getId(), ex);
        }

        try {
            MappingUtil.mapShippingAddressToRequest(request, ctCartLike.getShippingAddress());
        } catch (final NullPointerException ex) {
            LOG.warn("Could not map shipping details from payment with ID " + paymentWithCartLike.getPayment().getId(), ex);
        }

        return request;
    }
}
