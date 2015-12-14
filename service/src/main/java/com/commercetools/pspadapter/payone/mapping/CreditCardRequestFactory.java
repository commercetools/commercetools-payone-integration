package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CCPreauthorizationRequest;
import com.google.common.base.Preconditions;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.payments.Payment;

/**
 * @author fhaertig
 * @date 13.12.15
 */
public class CreditCardRequestFactory extends PayoneRequestFactory {

    @Override
    public CCPreauthorizationRequest createPreauthorizationRequest(
            final Payment ctPayment,
            final CartLike<?> ctCartLike,
            final PayoneConfig config) {

        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        String pseudocardpan = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.PSEUDOCARDPAN_KEY);
        CCPreauthorizationRequest request = new CCPreauthorizationRequest(config, pseudocardpan);

        //TODO: request.setReference(ctCartLike.getOrderNumber());
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
