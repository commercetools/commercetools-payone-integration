package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod;
import com.commercetools.pspadapter.payone.transaction.PaymentMethodDispatcher;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.payments.PaymentMethodInfo;

import java.util.Map;
import java.util.Optional;

public class PaymentDispatcher {

    private final Map<PaymentMethod, PaymentMethodDispatcher> methodDispatcher;

    private final String payoneInterfaceName;

    public PaymentDispatcher(final Map<PaymentMethod, PaymentMethodDispatcher> methodDispatcher,
                             final String payoneInterfaceName) {
        this.methodDispatcher = methodDispatcher;
        this.payoneInterfaceName = payoneInterfaceName;
    }

    public PaymentWithCartLike dispatchPayment(PaymentWithCartLike paymentWithCartLike) {
        final PaymentMethodInfo paymentMethodInfo = paymentWithCartLike.getPayment().getPaymentMethodInfo();

        if (!payoneInterfaceName.equals(paymentMethodInfo.getPaymentInterface())) {
            throw new IllegalArgumentException(String.format(
                    "unsupported payment interface '%s'", paymentMethodInfo.getPaymentInterface()));
        }

        if (paymentMethodInfo.getMethod() == null) {
            throw new IllegalArgumentException("No payment method provided");
        }

        return Optional.of(PaymentMethod.fromMethodKey(paymentMethodInfo.getMethod()))
                .map(methodDispatcher::get)
                .map(dispatcher -> {
                    try {
                        return dispatcher.dispatchPayment(paymentWithCartLike);
                    } catch (final ConcurrentModificationException cme) {
                        throw new java.util.ConcurrentModificationException("The payment could not be dispatched: "
                                + cme.getMessage(), cme);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "Unsupported payment method '%s'", paymentMethodInfo.getMethod())));
    }
}
