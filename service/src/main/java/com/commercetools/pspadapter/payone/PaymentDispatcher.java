package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.MethodKeys;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.sepa.PaymentMethodDispatcher;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentMethodInfo;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class PaymentDispatcher implements Consumer<PaymentWithCartLike> {

    private final Map<String, PaymentMethodDispatcher> methodDispatcherMap;

    public PaymentDispatcher(Map<String, PaymentMethodDispatcher> methodDispatcherMap) {
        this.methodDispatcherMap = methodDispatcherMap;
    }

    @Override
    public void accept(PaymentWithCartLike paymentWithCartLike) {
        try {
            dispatchPayment(paymentWithCartLike);
        } catch (Exception e) {
            // TODO: Log errors
        }
    }

    public PaymentWithCartLike dispatchPayment(PaymentWithCartLike paymentWithCartLike) {
        final PaymentMethodInfo paymentMethodInfo = paymentWithCartLike.getPayment().getPaymentMethodInfo();

        if (!"PAYONE".equals(paymentMethodInfo.getPaymentInterface()))
            throw new IllegalArgumentException("Unsupported Payment Interface");

        if (paymentMethodInfo.getMethod() == null)
            throw new IllegalArgumentException("No Payment Method provided");

        return Optional.ofNullable(methodDispatcherMap.get(paymentMethodInfo.getMethod()))
            .map(md -> md.dispatchPayment(paymentWithCartLike))
            .orElseThrow(() -> new IllegalArgumentException("Unsupported Payment Method"));
    }
}
