package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethodDispatcher;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.PaymentMethodInfo;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class PaymentDispatcher implements Consumer<PaymentWithCartLike> {

    private static final Logger LOG = LogManager.getLogger(PaymentDispatcher.class);

    private final ImmutableMap<PaymentMethod, PaymentMethodDispatcher> methodDispatcherMap;

    public PaymentDispatcher(final Map<PaymentMethod, PaymentMethodDispatcher> methodDispatcherMap) {
        this.methodDispatcherMap = ImmutableMap.copyOf(methodDispatcherMap);
    }

    @Override
    public void accept(PaymentWithCartLike paymentWithCartLike) {
        try {
            dispatchPayment(paymentWithCartLike);
        } catch (Exception e) {
            LOG.error("Error dispatching payment with id " + paymentWithCartLike.getPayment().getId(), e);
        }
    }

    public PaymentWithCartLike dispatchPayment(PaymentWithCartLike paymentWithCartLike) {
        final PaymentMethodInfo paymentMethodInfo = paymentWithCartLike.getPayment().getPaymentMethodInfo();

        if (!"PAYONE".equals(paymentMethodInfo.getPaymentInterface())) {
            throw new IllegalArgumentException("Unsupported Payment Interface");
        }

        if (paymentMethodInfo.getMethod() == null) {
            throw new IllegalArgumentException("No Payment Method provided");
        }

        return Optional.ofNullable(methodDispatcherMap.get(PaymentMethod.getMethodFromString(paymentMethodInfo.getMethod())))
            .map(methodDispatcher -> methodDispatcher.dispatchPayment(paymentWithCartLike))
            .orElseThrow(() -> new IllegalArgumentException("Unsupported Payment Method"));
    }
}
