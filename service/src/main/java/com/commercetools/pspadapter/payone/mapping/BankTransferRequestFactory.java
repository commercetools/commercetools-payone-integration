package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.google.common.base.Preconditions;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.payments.Payment;
import org.javamoney.moneta.function.MonetaryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author fhaertig
 * @since 22.01.16
 */
public class BankTransferRequestFactory extends PayoneRequestFactory {

    private static final Logger LOG = LoggerFactory.getLogger(BankTransferRequestFactory.class);

    public BankTransferRequestFactory(final PayoneConfig config) {
        super(config);
    }

    @Override
    public BankTransferAuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {


        final Payment ctPayment = paymentWithCartLike.getPayment();
        final CartLike ctCartLike = paymentWithCartLike.getCartLike();

        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String clearingSubType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod()).getSubType();
        BankTransferAuthorizationRequest request = new BankTransferAuthorizationRequest(getConfig(), clearingSubType);

        request.setIban(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.IBAN_FIELD));
        request.setBic(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.BIC_FIELD));

        request.setReference(paymentWithCartLike.getReference());

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
}
