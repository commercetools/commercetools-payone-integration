package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.MethodKeys;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.TransactionExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethodDispatcher;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.payments.Transaction;
import org.junit.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.*;

public class PaymentDispatcherTest extends PaymentTestHelper {
    private class CountingPaymentMethodDispatcher extends PaymentMethodDispatcher {
        public int count = 0;

        public CountingPaymentMethodDispatcher() {
            super(new TransactionExecutor() {
                @Override
                public PaymentWithCartLike executeTransaction(PaymentWithCartLike payment, Transaction transaction) {
                    return payment;
                }
            }, new HashMap<>());
        }

        @Override
        public PaymentWithCartLike dispatchPayment(PaymentWithCartLike payment) {
            count += 1;
            return super.dispatchPayment(payment);
        }
    }

    @Test
    public void testRefusingWrongPaymentInterface() throws Exception {
        PaymentDispatcher dispatcher = new PaymentDispatcher(null);

        final Throwable noInterface = catchThrowable(() -> dispatcher.dispatchPayment(new PaymentWithCartLike(dummyPaymentNoInterface(), (Cart)null)));
        assertThat(noInterface).isInstanceOf(IllegalArgumentException.class);

        final Throwable wrongInterface = catchThrowable(() -> dispatcher.dispatchPayment(new PaymentWithCartLike(dummyPaymentWrongInterface(), (Cart)null)));
        assertThat(wrongInterface).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testRefusingUnknownPaymentMethod() throws Exception {
        PaymentDispatcher dispatcher = new PaymentDispatcher(new HashMap<>());

        final Throwable noInterface = catchThrowable(() -> dispatcher.dispatchPayment(new PaymentWithCartLike(dummyPaymentUnknownMethod(), (Cart)null)));
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
        dispatcher.dispatchPayment(new PaymentWithCartLike(dummyPaymentTwoTransactionsPending(), (Cart)null));
        dispatcher.dispatchPayment(new PaymentWithCartLike(dummyPaymentTwoTransactionsSuccessPending(), (Cart)null));

        assertThat(creditCardDispatcher.count).isEqualTo(2);
        assertThat(sepaDispatcher.count).isEqualTo(0);
    }
}