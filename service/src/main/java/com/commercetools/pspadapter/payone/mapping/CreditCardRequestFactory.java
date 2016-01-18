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

import java.util.Optional;

/**
 * @author fhaertig
 * @since 13.12.15
 */
public class CreditCardRequestFactory extends PayoneRequestFactory {

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

        if (paymentWithCartLike.getOrderNumber().isPresent()) {
            request.setReference(paymentWithCartLike.getOrderNumber().get());
        }

        request.setAmount(MonetaryUtil.minorUnits().queryFrom(ctPayment.getAmountPlanned()).intValue());
        request.setCurrency(ctPayment.getAmountPlanned().getCurrency().getCurrencyCode());
        request.setNarrative_text(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.REFERENCE_TEXT_FIELD));
        request.setUserid(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.USER_ID_FIELD));

        MappingUtil.mapCustomerToRequest(request, ctPayment.getCustomer());
        MappingUtil.mapBillingAddressToRequest(request, ctCartLike.getBillingAddress());
        MappingUtil.mapShippingAddressToRequest(request, ctCartLike.getShippingAddress());
        return request;
    }

    @Override
    public CreditCardAuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {

        final Payment ctPayment = paymentWithCartLike.getPayment();
        final CartLike ctCartLike = paymentWithCartLike.getCartLike();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");


        final String pseudocardpan = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD);
        CreditCardAuthorizationRequest request = new CreditCardAuthorizationRequest(getConfig(), pseudocardpan);

        request.setAmount(MonetaryUtil.minorUnits().queryFrom(ctPayment.getAmountPlanned()).intValue());
        request.setCurrency(ctPayment.getAmountPlanned().getCurrency().getCurrencyCode());
        request.setNarrative_text(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.REFERENCE_TEXT_FIELD));
        request.setUserid(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.USER_ID_FIELD));

        MappingUtil.mapCustomerToRequest(request, ctPayment.getCustomer());
        MappingUtil.mapBillingAddressToRequest(request, ctCartLike.getBillingAddress());
        MappingUtil.mapShippingAddressToRequest(request, ctCartLike.getShippingAddress());

        return request;
    }

    @Override
    public CreditCardCaptureRequest createCaptureRequest(final PaymentWithCartLike paymentWithCartLike, final int sequenceNumber) {

        final Payment ctPayment = paymentWithCartLike.getPayment();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        CreditCardCaptureRequest request = new CreditCardCaptureRequest(getConfig());

        request.setTxid(ctPayment.getInterfaceId());

        request.setSequencenumber(sequenceNumber);
        request.setAmount(MonetaryUtil.minorUnits().queryFrom(ctPayment.getAmountAuthorized()).intValue());
        Optional
            .ofNullable(ctPayment.getAmountAuthorized())
            .ifPresent(m -> request.setCurrency(m.getCurrency().getCurrencyCode()));

        return request;
    }
}
