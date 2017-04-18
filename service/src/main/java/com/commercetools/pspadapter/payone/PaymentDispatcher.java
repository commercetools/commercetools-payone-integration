package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod;
import com.commercetools.pspadapter.payone.transaction.PaymentMethodDispatcher;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.payments.PaymentMethodInfo;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.commercetools.pspadapter.payone.util.PayoneConstants.PAYONE;

public class PaymentDispatcher implements Consumer<PaymentWithCartLike> {

    private final Map<PaymentMethod, PaymentMethodDispatcher> methodDispatcher;

    public PaymentDispatcher(final Map<PaymentMethod, PaymentMethodDispatcher> methodDispatcher) {
        this.methodDispatcher = methodDispatcher;
    }

    @Override
    public void accept(PaymentWithCartLike paymentWithCartLike) {
        dispatchPayment(paymentWithCartLike);
    }

    public PaymentWithCartLike dispatchPayment(PaymentWithCartLike paymentWithCartLike) {
        final PaymentMethodInfo paymentMethodInfo = paymentWithCartLike.getPayment().getPaymentMethodInfo();

        if (!PAYONE.equals(paymentMethodInfo.getPaymentInterface())) {
            throw new IllegalArgumentException(String.format(
                    "unsupported payment interface '%s'", paymentMethodInfo.getPaymentInterface()));
        }

        if (paymentMethodInfo.getMethod() == null) {
            throw new IllegalArgumentException("No payment method provided");
        }

        return Optional.ofNullable(methodDispatcher.get(PaymentMethod.fromMethodKey(paymentMethodInfo.getMethod())))
            .map(methodDispatcher -> {
                try {
                    return methodDispatcher.dispatchPayment(paymentWithCartLike);
                } catch (final ConcurrentModificationException cme) {
                    throw new java.util.ConcurrentModificationException
                            ("The payment could not be dispatched: " + cme.getMessage(), cme);
                }
            })
            .orElseThrow(() -> new IllegalArgumentException(String.format(
                    "Unsupported payment method '%s'", paymentMethodInfo.getMethod())));
    }
}
