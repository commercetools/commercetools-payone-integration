package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.CaptureRequest;
import com.commercetools.pspadapter.tenant.TenantConfig;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.payments.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import static com.commercetools.pspadapter.payone.mapping.MappingUtil.getGenderFromPaymentCart;
import static com.commercetools.pspadapter.payone.mapping.MappingUtil.getPaymentLanguage;
import static com.commercetools.pspadapter.tenant.TenantLoggerUtil.createLoggerName;

/**
 * @author fhaertig
 * @since 14.12.15
 */
public abstract class PayoneRequestFactory {

    private TenantConfig tenantConfig;

    private final Logger logger;

    @Nonnull
    public TenantConfig getTenantConfig() {
        return tenantConfig;
    }

    @Nonnull
    public PayoneConfig getPayoneConfig() {
        return tenantConfig.getPayoneConfig();
    }

    protected PayoneRequestFactory(@Nonnull final TenantConfig tenantConfig) {
        this.tenantConfig = tenantConfig;
        this.logger = LoggerFactory.getLogger(createLoggerName(this.getClass(), tenantConfig.getName()));
    }

    @Nonnull
    public AuthorizationRequest createPreauthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        throw new UnsupportedOperationException("this request type is not supported by this payment method.");
    }

    @Nonnull
    public AuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        throw new UnsupportedOperationException("this request type is not supported by this payment method.");
    }

    public CaptureRequest createCaptureRequest(final PaymentWithCartLike paymentWithCartLike, final int sequenceNumber) {
        throw new UnsupportedOperationException("this request type is not supported by this payment method.");
    }

    /**
     * Map the next values from {@code paymentWithCartLike} to {@code request}:<ul>
     *     <li>amount planned</li>
     *     <li>order reference</li>
     *     <li>custom fields</li>
     *     <li>{@link io.sphere.sdk.customers.Customer} info if exists</li>
     *     <li>billing and shipping address</li>
     *     <li>language</li>
     * </ul>
     *
     * @param request             {@link AuthorizationRequest} instance to which set the values
     * @param paymentWithCartLike cart/order info from which read the values
     */
    protected void mapFormPaymentWithCartLike(final AuthorizationRequest request,
                                              final PaymentWithCartLike paymentWithCartLike) {
        final Payment ctPayment = paymentWithCartLike.getPayment();
        final CartLike ctCartLike = paymentWithCartLike.getCartLike();

        request.setReference(paymentWithCartLike.getReference());

        MappingUtil.mapCustomFieldsFromPayment(request, ctPayment.getCustom());

        MappingUtil.mapAmountPlannedFromPayment(request, ctPayment);

        try {
            MappingUtil.mapCustomerToRequest(request, ctPayment.getCustomer());
        } catch (final IllegalArgumentException ex) {
            logger.debug("Could not fully map customer in payment with ID {} {}",
                    paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        try {
            MappingUtil.mapBillingAddressToRequest(request, ctCartLike.getBillingAddress());
        } catch (final IllegalArgumentException ex) {
            logger.error("Could not fully map billing address in payment with ID {} {}",
                    paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        try {
            MappingUtil.mapShippingAddressToRequest(request, ctCartLike.getShippingAddress());
        } catch (final IllegalArgumentException ex) {
            logger.debug("Could not fully map shipping address in payment with ID {} {}",
                    paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        //customer's locale, if set in custom field or cartLike
        getPaymentLanguage(paymentWithCartLike).ifPresent(request::setLanguage);

        //gender: Payone supports one of 2 characters [m, f]
        getGenderFromPaymentCart(paymentWithCartLike).ifPresent(request::setGender);
    }

}
