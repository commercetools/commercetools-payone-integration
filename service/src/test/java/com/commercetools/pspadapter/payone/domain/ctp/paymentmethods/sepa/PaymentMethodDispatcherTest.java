package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.sepa;

import static org.hamcrest.CoreMatchers.is;

import com.commercetools.pspadapter.payone.PaymentTestHelper;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethodDispatcher;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.TransactionExecutor;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionType;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.CompletionException;

import static org.junit.Assert.*;

public class PaymentMethodDispatcherTest extends PaymentTestHelper {
    private interface CountingTransactionExecutor extends TransactionExecutor {
        int getCount();
    }

    private CountingTransactionExecutor countingTransactionExecutor() {
        return new CountingTransactionExecutor() {
            int count = 0;
            @Override
            public PaymentWithCartLike executeTransaction(PaymentWithCartLike payment, Transaction transaction) {
                count += 1;
                return payment;
            }
            @Override
            public int getCount() {
                return count;
            }
        };
    }

    private CountingTransactionExecutor returnSuccessTransactionExecutor(final int afterExecutions) {
        return new CountingTransactionExecutor() {
            int count = 0;
            @Override
            public PaymentWithCartLike executeTransaction(PaymentWithCartLike payment, Transaction transaction) {
                count += 1;
                if (count > afterExecutions) try {
                    return payment.withPayment(dummyPaymentTwoTransactionsSuccessPending());
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
                else return payment;
            }
            @Override
            public int getCount() {
                return count;
            }
        };
    }

    @Test
    public void usesDefaultExecutor() throws Exception {
        CountingTransactionExecutor countingTransactionExecutor = countingTransactionExecutor();
        PaymentMethodDispatcher dispatcher = new PaymentMethodDispatcher(countingTransactionExecutor, new HashMap<>());
        dispatcher.dispatchPayment(new PaymentWithCartLike(dummyPaymentTwoTransactionsPending(), (Cart)null));
        assertThat(countingTransactionExecutor.getCount(), is(1));
    }

    @Test
    public void callsCorrectExecutor() throws Exception {
        CountingTransactionExecutor defaultExecutor = countingTransactionExecutor();
        CountingTransactionExecutor chargeExecutor = countingTransactionExecutor();
        CountingTransactionExecutor refundExecutor = countingTransactionExecutor();
        final HashMap<TransactionType, TransactionExecutor> executorMap = new HashMap<>();
        executorMap.put(TransactionType.CHARGE, chargeExecutor);
        executorMap.put(TransactionType.REFUND, refundExecutor);
        PaymentMethodDispatcher dispatcher = new PaymentMethodDispatcher(defaultExecutor, executorMap);
        dispatcher.dispatchPayment(new PaymentWithCartLike(dummyPaymentTwoTransactionsPending(), (Cart)null));
        assertThat(defaultExecutor.getCount(), is(0));
        assertThat(chargeExecutor.getCount(), is(1));
        assertThat(refundExecutor.getCount(), is(0));
    }

    @Test
    public void callsSecondExecutorAfterFirstTransactionStateChanged() throws Exception {
        CountingTransactionExecutor defaultExecutor = countingTransactionExecutor();
        CountingTransactionExecutor chargeExecutor = returnSuccessTransactionExecutor(2);
        CountingTransactionExecutor refundExecutor = countingTransactionExecutor();
        final HashMap<TransactionType, TransactionExecutor> executorMap = new HashMap<>();
        executorMap.put(TransactionType.CHARGE, chargeExecutor);
        executorMap.put(TransactionType.REFUND, refundExecutor);
        PaymentMethodDispatcher dispatcher = new PaymentMethodDispatcher(defaultExecutor, executorMap);
        dispatcher.dispatchPayment(dispatcher.dispatchPayment(dispatcher.dispatchPayment(new PaymentWithCartLike(dummyPaymentTwoTransactionsPending(), (Cart)null))));
        assertThat(defaultExecutor.getCount(), is(0));
        assertThat(chargeExecutor.getCount(), is(3));
        assertThat(refundExecutor.getCount(), is(1));
    }
}