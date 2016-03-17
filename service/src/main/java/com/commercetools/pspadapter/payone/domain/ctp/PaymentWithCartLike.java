package com.commercetools.pspadapter.payone.domain.ctp;

import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.payments.Payment;

import java.util.Optional;

/**
 * Wraps a payment and an order or a cart together
 * and provides easy access to a reference number (e.g. an order number).
 * This reference is extracted from the custom field {@link com.commercetools.pspadapter.payone.mapping.CustomFieldKeys REFERENCE_FIELD}
 * at the payment.
 *
 * @author cneijenhuis
 * @author fhaertig
 * @since 14.12.15
 */
public class PaymentWithCartLike {
    private final Payment payment;
    private final CartLike<?> cartLike;
    private final String reference;

    /**
     * Creates a wrapper object for a payment and the belonging order/cart.
     *
     * @param payment the payment object to use
     * @param cartLike the order or cart which references the payment
     * @throws IllegalStateException if the payment is missing the custom field {@link com.commercetools.pspadapter.payone.mapping.CustomFieldKeys REFERENCE_FIELD}
     */
    public PaymentWithCartLike(final Payment payment, final CartLike<?> cartLike) {
        this.reference = Optional.ofNullable(payment.getCustom())
                .map(customFields -> customFields.getFieldAsString(CustomFieldKeys.REFERENCE_FIELD))
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "Payment with id '%s' is missing the required custom field '%s'",
                        payment.getId(),
                        CustomFieldKeys.REFERENCE_FIELD)));
        this.payment = payment;
        this.cartLike = cartLike;
    }

    public Payment getPayment() {
        return payment;
    }

    public CartLike<?> getCartLike() {
        return cartLike;
    }

    public String getReference() {
        return reference;
    }

    public PaymentWithCartLike withPayment(final Payment payment) {
        return new PaymentWithCartLike(payment, cartLike);
    }
}
