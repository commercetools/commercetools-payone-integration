package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CCPreauthorizationRequest;
import com.google.common.base.Preconditions;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.payments.Payment;

/**
 * @author fhaertig
 * @date 13.12.15
 */
public class CreditCardRequestFactory extends PayoneRequestFactory {

    public CreditCardRequestFactory(final PayoneConfig config) {
        super(config);
    }

    @Override
    public CCPreauthorizationRequest createPreauthorizationRequest(
            final PaymentWithCartLike paymentWithCartLike) {

        final Payment ctPayment = paymentWithCartLike.getPayment();
        final CartLike ctCartLike = paymentWithCartLike.getCartLike();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        String pseudocardpan = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.PSEUDOCARDPAN_KEY);
        CCPreauthorizationRequest request = new CCPreauthorizationRequest(getConfig(), pseudocardpan);

        if (paymentWithCartLike.getOrderNumber().isPresent()) {
            request.setReference(paymentWithCartLike.getOrderNumber().get());
        }

        request.setAmount(ctPayment.getAmountPlanned().getNumber().intValueExact());
        request.setCurrency(ctPayment.getAmountPlanned().getCurrency().getCurrencyCode());
        request.setNarrative_text(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.NARRATIVETEXT_KEY));
        request.setUserid(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.USERID_KEY));

        request = (CCPreauthorizationRequest) MappingUtil.mapCustomerToRequest(request, ctPayment.getCustomer());
        request = (CCPreauthorizationRequest) MappingUtil.mapBillingAddressToRequest(request, ctCartLike.getBillingAddress());
        request = (CCPreauthorizationRequest) MappingUtil.mapShippingAddressToRequest(request, ctCartLike.getShippingAddress());
        return request;
    }

}
