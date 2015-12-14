package com.commercetools.pspadapter.payone.domain.ctp;

import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;

import java.util.Optional;

public class PaymentWithCartLike {
    private final Payment payment;
    private final CartLike<?> cartLike;
    private final Optional<String> orderNumber;

    public PaymentWithCartLike(final Payment payment, final Order order) {
        this(payment, order, Optional.ofNullable(order.getOrderNumber()));
    }

    public PaymentWithCartLike(final Payment payment, final Cart cart) {
        this(payment, cart, Optional.empty());
    }

    private PaymentWithCartLike(Payment payment, CartLike<?> cartLike, Optional<String> orderNumber) {
        this.payment = payment;
        this.cartLike = cartLike;
        this.orderNumber = orderNumber;
    }

    public Payment getPayment() {
        return payment;
    }

    public CartLike<?> getCartLike() {
        return cartLike;
    }

    public Optional<String> getOrderNumber() {
        return orderNumber;
    }

    public PaymentWithCartLike withPayment(final Payment payment) {
        return new PaymentWithCartLike(payment, cartLike, orderNumber);
    }
}
