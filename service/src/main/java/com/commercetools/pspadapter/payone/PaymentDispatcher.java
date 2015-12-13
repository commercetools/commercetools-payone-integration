package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.MethodKeys;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.sepa.PaymentMethodDispatcher;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentMethodInfo;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class PaymentDispatcher implements Consumer<Payment> {

    private final Map<String, PaymentMethodDispatcher> methodDispatcherMap;

    public PaymentDispatcher(Map<String, PaymentMethodDispatcher> methodDispatcherMap) {
        this.methodDispatcherMap = methodDispatcherMap;
    }

    @Override
    public void accept(Payment payment) {
        try {
            dispatchPayment(payment);
        } catch (Exception e) {
            // TODO: Log errors
        }
    }

    public Payment dispatchPayment(Payment payment) {
        final PaymentMethodInfo paymentMethodInfo = payment.getPaymentMethodInfo();

        if (paymentMethodInfo.getPaymentInterface() == null || !paymentMethodInfo.getPaymentInterface().equals("PAYONE"))
            throw new IllegalArgumentException("Unsupported Payment Interface");

        if (paymentMethodInfo.getMethod() == null)
            throw new IllegalArgumentException("No Payment Method provided");

        return Optional.ofNullable(methodDispatcherMap.get(paymentMethodInfo.getMethod()))
            .map(md -> md.dispatchPayment(payment))
            .orElseThrow(() -> new IllegalArgumentException("Unsupported Payment Method"));
    }
}
