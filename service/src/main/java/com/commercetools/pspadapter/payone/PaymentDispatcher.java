package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethodDispatcher;
import io.sphere.sdk.payments.PaymentMethodInfo;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class PaymentDispatcher implements Consumer<PaymentWithCartLike> {

    private static final Logger LOG = LogManager.getLogger(PaymentDispatcher.class);

    private final Map<PaymentMethod, PaymentMethodDispatcher> methodDispatcherMap;

    public PaymentDispatcher(Map<PaymentMethod, PaymentMethodDispatcher> methodDispatcherMap) {
        this.methodDispatcherMap = methodDispatcherMap;
    }

    @Override
    public void accept(PaymentWithCartLike paymentWithCartLike) {
        try {
            dispatchPayment(paymentWithCartLike);
        } catch (Exception e) {
            LOG.error("Error dispatching payment with id " + paymentWithCartLike.getPayment().getId(), e);
        }
    }

    public DispatchResult dispatchPayment(PaymentWithCartLike paymentWithCartLike) {
        final PaymentMethodInfo paymentMethodInfo = paymentWithCartLike.getPayment().getPaymentMethodInfo();

        if (!"PAYONE".equals(paymentMethodInfo.getPaymentInterface()))
            return DispatchResult.of(400, "Unsupported Payment Interface");

        if (paymentMethodInfo.getMethod() == null)
            return DispatchResult.of(400, "No Payment Method provided");

        Optional.ofNullable(methodDispatcherMap.get(PaymentMethod.getMethodFromString(paymentMethodInfo.getMethod())))
            .map(md -> md.dispatchPayment(paymentWithCartLike))
            .orElseThrow(() -> new IllegalArgumentException("Unsupported Payment Method"));

        return DispatchResult.of(200, "Successfully handled the given payment");
    }
}
