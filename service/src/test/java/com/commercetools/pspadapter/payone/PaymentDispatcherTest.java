package com.commercetools.pspadapter.payone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethodDispatcher;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.TransactionExecutor;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.payments.Transaction;
import org.junit.Test;
import util.PaymentTestHelper;

import java.util.HashMap;

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

        DispatchResult result = dispatcher.dispatchPayment(new PaymentWithCartLike(dummyPaymentNoInterface(), (Cart) null));
        assertThat(result.getStatusCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("Unsupported Payment Interface");

        DispatchResult result2 = dispatcher.dispatchPayment(new PaymentWithCartLike(dummyPaymentWrongInterface(), (Cart) null));
        assertThat(result2.getStatusCode()).isEqualTo(400);
        assertThat(result2.getMessage()).isEqualTo("Unsupported Payment Interface");
    }

    @Test
    public void testRefusingUnknownPaymentMethod() throws Exception {
        PaymentDispatcher dispatcher = new PaymentDispatcher(new HashMap<>());

        final Throwable noInterface = catchThrowable(() -> dispatcher.dispatchPayment(new PaymentWithCartLike(dummyPaymentUnknownMethod(), (Cart) null)));
        assertThat(noInterface).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testDispatchCorrectly() throws Exception {
        final CountingPaymentMethodDispatcher creditCardDispatcher = new CountingPaymentMethodDispatcher();
        final CountingPaymentMethodDispatcher sepaDispatcher = new CountingPaymentMethodDispatcher();

        final HashMap<PaymentMethod, PaymentMethodDispatcher> methodDispatcherMap = new HashMap<>();
        methodDispatcherMap.put(PaymentMethod.CREDIT_CARD, creditCardDispatcher);
        methodDispatcherMap.put(PaymentMethod.DIRECT_DEBIT_SEPA, sepaDispatcher);

        PaymentDispatcher dispatcher = new PaymentDispatcher(methodDispatcherMap);
        dispatcher.dispatchPayment(new PaymentWithCartLike(dummyPaymentTwoTransactionsPending(), (Cart)null));
        dispatcher.dispatchPayment(new PaymentWithCartLike(dummyPaymentTwoTransactionsSuccessPending(), (Cart)null));

        assertThat(creditCardDispatcher.count).isEqualTo(2);
        assertThat(sepaDispatcher.count).isEqualTo(0);
    }
}