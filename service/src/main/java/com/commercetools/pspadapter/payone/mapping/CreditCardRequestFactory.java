package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardCaptureRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardPreauthorizationRequest;
import com.google.common.base.Preconditions;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.payments.Payment;
import org.javamoney.moneta.function.MonetaryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author fhaertig
 * @since 13.12.15
 */
public class CreditCardRequestFactory extends PayoneRequestFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CreditCardRequestFactory.class);

    public CreditCardRequestFactory(final PayoneConfig config) {
        super(config);
    }

    @Override
    public CreditCardPreauthorizationRequest createPreauthorizationRequest(
            final PaymentWithCartLike paymentWithCartLike) {

        final Payment ctPayment = paymentWithCartLike.getPayment();
        final CartLike ctCartLike = paymentWithCartLike.getCartLike();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String pseudocardpan = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD);
        CreditCardPreauthorizationRequest request = new CreditCardPreauthorizationRequest(getConfig(), pseudocardpan);

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
            MappingUtil.mapBillingAddressToRequest(request, ctCartLike.getBillingAddress());
            MappingUtil.mapShippingAddressToRequest(request, ctCartLike.getShippingAddress());
        } catch (final IllegalArgumentException ex) {
            LOG.warn(String.format("Could not fully map payment with ID '%s'. Cause: %s", paymentWithCartLike.getPayment().getId(), ex.getMessage()));
        }

        return request;
    }

    @Override
    public CreditCardAuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {

        final Payment ctPayment = paymentWithCartLike.getPayment();
        final CartLike ctCartLike = paymentWithCartLike.getCartLike();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String pseudocardpan = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD);
        CreditCardAuthorizationRequest request = new CreditCardAuthorizationRequest(getConfig(), pseudocardpan);

        Optional.ofNullable(ctPayment.getAmountPlanned())
                .ifPresent(m -> request.setAmount(MonetaryUtil
                                                    .minorUnits()
                                                    .queryFrom(ctPayment.getAmountPlanned())
                                                    .intValue()));
        request.setCurrency(ctPayment.getAmountPlanned().getCurrency().getCurrencyCode());
        request.setNarrative_text(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.REFERENCE_TEXT_FIELD));
        request.setUserid(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.USER_ID_FIELD));

        try {
            MappingUtil.mapCustomerToRequest(request, ctPayment.getCustomer());
            MappingUtil.mapBillingAddressToRequest(request, ctCartLike.getBillingAddress());
            MappingUtil.mapShippingAddressToRequest(request, ctCartLike.getShippingAddress());
        } catch (final IllegalArgumentException ex) {
            LOG.warn(String.format("Could not fully map payment with ID '%s'. Cause: %s", paymentWithCartLike.getPayment().getId(), ex.getMessage()));
        }

        return request;
    }

    @Override
    public CreditCardCaptureRequest createCaptureRequest(final PaymentWithCartLike paymentWithCartLike, final int sequenceNumber) {

        final Payment ctPayment = paymentWithCartLike.getPayment();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        CreditCardCaptureRequest request = new CreditCardCaptureRequest(getConfig());

        request.setTxid(ctPayment.getInterfaceId());

        request.setSequencenumber(sequenceNumber);
        Optional.ofNullable(ctPayment.getAmountAuthorized())
                .ifPresent(amount -> {
                    request.setCurrency(amount.getCurrency().getCurrencyCode());
                    request.setAmount(MonetaryUtil
                            .minorUnits()
                            .queryFrom(amount)
                            .intValue());
                });

        return request;
    }
}
