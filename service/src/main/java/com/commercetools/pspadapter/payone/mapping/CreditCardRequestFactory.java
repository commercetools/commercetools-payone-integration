package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardCaptureRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardPreauthorizationRequest;
import com.google.common.base.Preconditions;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import org.javamoney.moneta.function.MonetaryUtil;

import java.util.Optional;

/**
 * @author fhaertig
 * @date 13.12.15
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

        String pseudocardpan = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.PSEUDOCARDPAN_KEY);
        CreditCardPreauthorizationRequest request = new CreditCardPreauthorizationRequest(getConfig(), pseudocardpan);

        if (paymentWithCartLike.getOrderNumber().isPresent()) {
            request.setReference(paymentWithCartLike.getOrderNumber().get());
        }

        request.setAmount(MonetaryUtil.minorUnits().queryFrom(ctPayment.getAmountPlanned()).intValue());
        request.setCurrency(ctPayment.getAmountPlanned().getCurrency().getCurrencyCode());
        request.setNarrative_text(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.NARRATIVETEXT_KEY));
        request.setUserid(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.USERID_KEY));

        MappingUtil.mapCustomerToRequest(request, ctPayment.getCustomer());
        MappingUtil.mapBillingAddressToRequest(request, ctCartLike.getBillingAddress());
        MappingUtil.mapShippingAddressToRequest(request, ctCartLike.getShippingAddress());
        return request;
    }

    @Override
    public CreditCardCaptureRequest createCaptureRequest(final PaymentWithCartLike paymentWithCartLike, final Transaction transaction) {

        final Payment ctPayment = paymentWithCartLike.getPayment();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        CreditCardCaptureRequest request = new CreditCardCaptureRequest(getConfig());

        request.setTxid(ctPayment.getInterfaceId());
        request.setSequencenumber(Integer.getInteger(transaction.getInteractionId()));
        request.setAmount(MonetaryUtil.minorUnits().queryFrom(ctPayment.getAmountAuthorized()).intValue());
        Optional
            .ofNullable(ctPayment.getAmountAuthorized())
            .ifPresent(m -> request.setCurrency(m.getCurrency().getCurrencyCode()));

        return request;
    }
}
