package com.commercetools.pspadapter.payone.domain.ctp;

import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.payments.Payment;

/**
 * Wraps a payment and an order or a cart together
 * and provides easy access to the order number
 * extracted from the custom field {@link com.commercetools.pspadapter.payone.mapping.CustomFieldKeys REFERENCE_FIELD}.
 *
 * @author cneijenhuis
 * @author fhaertig
 * @since 14.12.15
 */
public class PaymentWithCartLike {
    private final Payment payment;
    private final CartLike<?> cartLike;
    private final String orderNumber;

    public PaymentWithCartLike(final Payment payment, final CartLike<?> cartLike) {
        this.payment = payment;
        this.cartLike = cartLike;

        if (payment.getCustom() != null) {
            this.orderNumber = payment.getCustom().getFieldAsString(CustomFieldKeys.REFERENCE_FIELD);
        } else {
            this.orderNumber = null;
        }
    }

    public Payment getPayment() {
        return payment;
    }

    public CartLike<?> getCartLike() {
        return cartLike;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public PaymentWithCartLike withPayment(final Payment payment) {
        return new PaymentWithCartLike(payment, cartLike);
    }
}
