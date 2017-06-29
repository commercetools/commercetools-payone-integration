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

    public AuthorizationRequest createPreauthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        throw new UnsupportedOperationException("this request type is not supported by this payment method.");
    }

    public AuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        throw new UnsupportedOperationException("this request type is not supported by this payment method.");
    }

    public CaptureRequest createCaptureRequest(final PaymentWithCartLike paymentWithCartLike, final int sequenceNumber) {
        throw new UnsupportedOperationException("this request type is not supported by this payment method.");
    }

    protected void mapFormPaymentWithCartLike(final AuthorizationRequest request,
                                              final PaymentWithCartLike paymentWithCartLike) {
        final Payment ctPayment = paymentWithCartLike.getPayment();
        final CartLike ctCartLike = paymentWithCartLike.getCartLike();

        MappingUtil.mapCustomFieldsFromPayment(request, ctPayment.getCustom());

        try {
            MappingUtil.mapCustomerToRequest(request, ctPayment.getCustomer());
        } catch (final IllegalArgumentException ex) {
            logger.debug("Could not fully map payment with ID {} {}",
                    paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        try {
            MappingUtil.mapBillingAddressToRequest(request, ctCartLike.getBillingAddress());
        } catch (final IllegalArgumentException ex) {
            logger.error("Could not fully map payment with ID {} {}",
                    paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        try {
            MappingUtil.mapShippingAddressToRequest(request, ctCartLike.getShippingAddress());
        } catch (final IllegalArgumentException ex) {
            logger.debug("Could not fully map payment with ID {} {}",
                    paymentWithCartLike.getPayment().getId(), ex.getMessage());
        }

        //customer's locale, if set in custom field or cartLike
        MappingUtil.getPaymentLanguage(paymentWithCartLike).ifPresent(request::setLanguage);
    }

}
