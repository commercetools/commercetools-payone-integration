package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.MethodKeys;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.TransactionExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.sepa.PaymentMethodDispatcher;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionType;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

public class PaymentDispatcherTest extends PaymentTestHelper {
    private class CountingPaymentMethodDispatcher extends PaymentMethodDispatcher {
        public int count = 0;

        public CountingPaymentMethodDispatcher() {
            super(new TransactionExecutor() {
                @Override
                public Payment executeTransaction(Payment payment, Transaction transaction) {
                    return payment;
                }
            }, new HashMap<>());
        }

        @Override
        public Payment dispatchPayment(Payment payment) {
            count += 1;
            return super.dispatchPayment(payment);
        }
    }

    @Test
    public void testRefusingWrongPaymentInterface() throws Exception {
        PaymentDispatcher dispatcher = new PaymentDispatcher(null);

        final Throwable noInterface = catchThrowable(() -> dispatcher.dispatchPayment(dummyPaymentNoInterface()));
        assertThat(noInterface).isInstanceOf(IllegalArgumentException.class);

        final Throwable wrongInterface = catchThrowable(() -> dispatcher.dispatchPayment(dummyPaymentWrongInterface()));
        assertThat(wrongInterface).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testRefusingUnknownPaymentMethod() throws Exception {
        PaymentDispatcher dispatcher = new PaymentDispatcher(new HashMap<>());

        final Throwable noInterface = catchThrowable(() -> dispatcher.dispatchPayment(dummyPaymentUnknownMethod()));
        assertThat(noInterface).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testDispatchCorrectly() throws Exception {
        final CountingPaymentMethodDispatcher creditCardDispatcher = new CountingPaymentMethodDispatcher();
        final CountingPaymentMethodDispatcher sepaDispatcher = new CountingPaymentMethodDispatcher();

        final HashMap<String, PaymentMethodDispatcher> methodDispatcherMap = new HashMap<>();
        methodDispatcherMap.put(MethodKeys.CREDIT_CARD, creditCardDispatcher);
        methodDispatcherMap.put(MethodKeys.DIRECT_DEBIT_SEPA, sepaDispatcher);

        PaymentDispatcher dispatcher = new PaymentDispatcher(methodDispatcherMap);
        dispatcher.dispatchPayment(dummyPaymentTwoTransactionsPending());
        dispatcher.dispatchPayment(dummyPaymentTwoTransactionsSuccessPending());

        assertThat(creditCardDispatcher.count).isEqualTo(2);
        assertThat(sepaDispatcher.count).isEqualTo(0);
    }
}