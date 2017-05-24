package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.CaptureRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.google.common.base.Preconditions;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.payments.Payment;
import org.javamoney.moneta.function.MonetaryUtil;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * @author fhaertig
 * @since 14.12.15
 */
public abstract class PayoneRequestFactory {

    private PayoneConfig config;

    public PayoneConfig getPayoneConfig() {
        return config;
    }

    public PayoneRequestFactory(final PayoneConfig config) {
        this.config = config;
    }

    public AuthorizationRequest createPreauthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        throw new UnsupportedOperationException("this request type is not supported by this payment method.");
    }

    public AuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        throw new UnsupportedOperationException("this request type is not supported by this payment method.");
    }

    public CaptureRequest createCaptureRequest(final PaymentWithCartLike paymentWithCartLike, final int sequenceNumber) {
        throw new UnsupportedOperationException("this request type is not supported by this payment method.");
    }

    /**
     * Create basic (pre)authorization request(based on {@code requestGenerator} and map the next default values
     * from the payment:
     * <ul>
     *     <li>reference (orderNumber)</li>
     *     <li>currency and amount (if specified in the payment)</li>
     *     <li>custom fields, customer info, addresses, language and so on. See
     *     {@link #mapFormPaymentWithCartLike(AuthorizationRequest, PaymentWithCartLike, Logger)} for details</li>
     * </ul>
     * @param paymentWithCartLike payment to map. {@code paymentWithCartLike.getPayment()} must be set.
     * @param requestGenerator function to create request based on {@link PayoneConfig} and clearing type from the payment
     * @param logger descendant class logger reference to log mapping errors and warnings.
     * @param <T> Type of resulted authorization request, subclass of {@link AuthorizationRequest}
     * @return created subclass of {@link AuthorizationRequest} of type {@code T} with mapped default payment values.
     */
    protected <T extends AuthorizationRequest> T createBasicAuthorizationRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike,
                                                                                 @Nonnull final BiFunction<PayoneConfig, String, T> requestGenerator,
                                                                                 @Nonnull final Logger logger) {
        final Payment ctPayment = paymentWithCartLike.getPayment();

        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String clearingSubType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod()).getSubType();
        T request = requestGenerator.apply(getPayoneConfig(), clearingSubType);

        request.setReference(paymentWithCartLike.getReference());

        Optional.ofNullable(ctPayment.getAmountPlanned())
                .ifPresent(amount -> {
                    request.setCurrency(amount.getCurrency().getCurrencyCode());
                    request.setAmount(MonetaryUtil
                            .minorUnits()
                            .queryFrom(amount)
                            .intValue());
                });

        mapFormPaymentWithCartLike(request, paymentWithCartLike, logger);

        return request;
    }

    protected void mapFormPaymentWithCartLike(final AuthorizationRequest request,
                                              final PaymentWithCartLike paymentWithCartLike,
                                              final Logger logger) {
        final Payment ctPayment = paymentWithCartLike.getPayment();
        final CartLike ctCartLike = paymentWithCartLike.getCartLike();

        MappingUtil.mapCustomFieldsFromPayment(request, ctPayment.getCustom());

        try {
            MappingUtil.mapCustomerToRequest(request, ctPayment.getCustomer());
        } catch (final IllegalArgumentException ex) {
            logger.debug("Could not fully map payment with ID {} {}", paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        try {
            MappingUtil.mapBillingAddressToRequest(request, ctCartLike.getBillingAddress());
        } catch (final IllegalArgumentException ex) {
            logger.error("Could not fully map payment with ID {} {}", paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        try {
            MappingUtil.mapShippingAddressToRequest(request, ctCartLike.getShippingAddress());
        } catch (final IllegalArgumentException ex) {
            logger.debug("Could not fully map payment with ID {} {}", paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        //customer's locale, if set in custom field or cartLike
        MappingUtil.getPaymentLanguage(paymentWithCartLike).ifPresent(request::setLanguage);
    }

}
