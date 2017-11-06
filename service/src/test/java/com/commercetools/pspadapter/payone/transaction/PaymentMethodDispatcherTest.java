package com.commercetools.pspadapter.payone.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionType;
import org.junit.Test;
import util.PaymentTestHelper;

import java.util.concurrent.CompletionException;

public class PaymentMethodDispatcherTest {
    private final PaymentTestHelper payments = new PaymentTestHelper();

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
                    return payment.withPayment(payments.dummyPaymentTwoTransactionsSuccessPending());
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
        final CountingTransactionExecutor countingTransactionExecutor = countingTransactionExecutor();
        final PaymentMethodDispatcher dispatcher = new PaymentMethodDispatcher(countingTransactionExecutor, ImmutableMap.of());
        dispatcher.dispatchPayment(new PaymentWithCartLike(payments.dummyPaymentTwoTransactionsPending(), (Cart)null));
        assertThat(countingTransactionExecutor.getCount()).isEqualTo(1);
    }

    @Test
    public void callsCorrectExecutor() throws Exception {
        final CountingTransactionExecutor defaultExecutor = countingTransactionExecutor();
        final CountingTransactionExecutor chargeExecutor = countingTransactionExecutor();
        final CountingTransactionExecutor refundExecutor = countingTransactionExecutor();
        final PaymentMethodDispatcher dispatcher = new PaymentMethodDispatcher(
                defaultExecutor,
                ImmutableMap.of(
                        TransactionType.CHARGE, chargeExecutor,
                        TransactionType.REFUND, refundExecutor));

        dispatcher.dispatchPayment(new PaymentWithCartLike(payments.dummyPaymentTwoTransactionsPending(), (Cart)null));
        assertThat(defaultExecutor.getCount()).isEqualTo(0);
        assertThat(chargeExecutor.getCount()).isEqualTo(1);
        assertThat(refundExecutor.getCount()).isEqualTo(0);
    }

    @Test
    public void callsSecondExecutorAfterFirstTransactionStateChanged() throws Exception {
        final CountingTransactionExecutor defaultExecutor = countingTransactionExecutor();
        final CountingTransactionExecutor chargeExecutor = returnSuccessTransactionExecutor(2);
        final CountingTransactionExecutor refundExecutor = countingTransactionExecutor();
        final PaymentMethodDispatcher dispatcher = new PaymentMethodDispatcher(
                defaultExecutor,
                ImmutableMap.of(
                        TransactionType.CHARGE, chargeExecutor,
                        TransactionType.REFUND, refundExecutor));

        dispatcher.dispatchPayment(dispatcher.dispatchPayment(dispatcher.dispatchPayment(new PaymentWithCartLike(payments.dummyPaymentTwoTransactionsPending(), (Cart)null))));
        assertThat(defaultExecutor.getCount()).isEqualTo(0);
        assertThat(chargeExecutor.getCount()).isEqualTo(3);
        assertThat(refundExecutor.getCount()).isEqualTo(1);
    }
}
