package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.sepa.SepaDispatcher;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentMethodInfo;

import java.util.function.Consumer;

public class PaymentDispatcher implements Consumer<Payment> {

    private final SepaDispatcher sepaDispatcher;

    public PaymentDispatcher(SepaDispatcher sepaDispatcher) {
        this.sepaDispatcher = sepaDispatcher;
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

        if (paymentMethodInfo.getMethod().equals("DIRECT_DEBIT-SEPA"))
            return sepaDispatcher.dispatchPayment(payment);

        // TODO: Other payment methods

        throw new IllegalArgumentException("Unsupported Payment Method");
    }
}
