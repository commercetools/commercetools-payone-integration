package com.commercetools.pspadapter.payone.domain.ctp;

import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.types.CustomFields;

public class PaymentWithCartLike {
    private final Payment payment;
    private final CartLike<?> cartLike;
    private final String orderNumber;

    public PaymentWithCartLike(final Payment payment, final Order order) {
        this(payment, order, payment.getCustom());
    }

    public PaymentWithCartLike(final Payment payment, final Cart cart) {

        this(payment, cart, payment.getCustom());
    }

    private PaymentWithCartLike(final Payment payment, final CartLike<?> cartLike, final CustomFields customFields) {
        this.payment = payment;
        this.cartLike = cartLike;

        if (customFields != null) {
            this.orderNumber = customFields.getFieldAsString(CustomFieldKeys.REFERENCE_FIELD);
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
        return new PaymentWithCartLike(payment, cartLike, payment.getCustom());
    }
}
