package com.commercetools.pspadapter.payone.transaction;

import com.commercetools.payments.TransactionStateResolver;
import com.commercetools.payments.TransactionStateResolverImpl;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionType;
import org.junit.Test;
import util.PaymentTestHelper;

import java.util.concurrent.CompletionException;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class PaymentMethodDispatcherTest {
    private final PaymentTestHelper payments = new PaymentTestHelper();

    private TransactionStateResolver transactionStateResolver = new TransactionStateResolverImpl();

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

    private CountingTransactionExecutor returnSuccessTransactionExecutor(final int afterExecutions,
                                                                         final Payment successfullyExecutedPayment) {
        return new CountingTransactionExecutor() {
            int count = 0;

            @Override
            public PaymentWithCartLike executeTransaction(PaymentWithCartLike payment, Transaction transaction) {
                count += 1;
                if (count > afterExecutions) try {
                    return payment.withPayment(successfullyExecutedPayment);
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
    public void callsDefaultExecutorWithInitial() throws Exception {
        callsDefaultExecutorWithTransactionState(payments.dummyPaymentTwoTransactionsInitial());
    }

    @Test
    public void callsDefaultExecutorWithPending() throws Exception {
        callsDefaultExecutorWithTransactionState(payments.dummyPaymentTwoTransactionsPending());
    }

    private void callsDefaultExecutorWithTransactionState(Payment payment) throws Exception {
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, null);
        final CountingTransactionExecutor countingTransactionExecutor = countingTransactionExecutor();
        final PaymentMethodDispatcher dispatcher = new PaymentMethodDispatcher(countingTransactionExecutor, ImmutableMap.of(), transactionStateResolver);
        dispatcher.dispatchPayment(paymentWithCartLike);
        assertThat(countingTransactionExecutor.getCount()).isEqualTo(1);
    }

    @Test
    public void callsCorrectExecutorForInitial() throws Exception {
        callsCorrectExecutorForTransactionState(payments.dummyPaymentTwoTransactionsInitial());
    }

    @Test
    public void callsCorrectExecutorForPending() throws Exception {
        callsCorrectExecutorForTransactionState(payments.dummyPaymentTwoTransactionsPending());
    }

    private void callsCorrectExecutorForTransactionState(Payment payment) throws Exception {
        final CountingTransactionExecutor defaultExecutor = countingTransactionExecutor();
        final CountingTransactionExecutor chargeExecutor = countingTransactionExecutor();
        final CountingTransactionExecutor refundExecutor = countingTransactionExecutor();
        final PaymentMethodDispatcher dispatcher = new PaymentMethodDispatcher(
                defaultExecutor,
                ImmutableMap.of(
                        TransactionType.CHARGE, chargeExecutor,
                        TransactionType.REFUND, refundExecutor),
                transactionStateResolver);

        dispatcher.dispatchPayment(new PaymentWithCartLike(payment, null));
        assertThat(defaultExecutor.getCount()).isEqualTo(0);
        assertThat(chargeExecutor.getCount()).isEqualTo(1);
        assertThat(refundExecutor.getCount()).isEqualTo(0);
    }

    @Test
    public void callsSecondExecutorAfterFirstTransactionStateChangedFromInitial() throws Exception {
        callsSecondExecutorAfterFirstTransactionStateChanged(
                payments.dummyPaymentTwoTransactionsInitial(), payments.dummyPaymentTwoTransactionsSuccessInitial());
    }

    @Test
    public void callsSecondExecutorAfterFirstTransactionStateChangedFromPending() throws Exception {
        callsSecondExecutorAfterFirstTransactionStateChanged(payments.dummyPaymentTwoTransactionsPending(),
                payments.dummyPaymentTwoTransactionsSuccessPending());
    }

    public void callsSecondExecutorAfterFirstTransactionStateChanged(Payment paymentUnprocessed, Payment successfullyExecutedPayment) throws Exception {
        final CountingTransactionExecutor defaultExecutor = countingTransactionExecutor();
        final CountingTransactionExecutor chargeExecutor = returnSuccessTransactionExecutor(2, successfullyExecutedPayment);
        final CountingTransactionExecutor refundExecutor = countingTransactionExecutor();
        final PaymentMethodDispatcher dispatcher = new PaymentMethodDispatcher(
                defaultExecutor,
                ImmutableMap.of(
                        TransactionType.CHARGE, chargeExecutor,
                        TransactionType.REFUND, refundExecutor),
                transactionStateResolver);

        dispatcher.dispatchPayment(dispatcher.dispatchPayment(dispatcher.dispatchPayment(new PaymentWithCartLike(paymentUnprocessed, null))));
        assertThat(defaultExecutor.getCount()).isEqualTo(0);
        assertThat(chargeExecutor.getCount()).isEqualTo(3);
        assertThat(refundExecutor.getCount()).isEqualTo(1);
    }

    @Test
    public void doesNotCallExecutorForSuccessAndFailTransactions() throws Exception {
        final CountingTransactionExecutor defaultExecutor = countingTransactionExecutor();
        final CountingTransactionExecutor chargeExecutor = countingTransactionExecutor();
        final CountingTransactionExecutor refundExecutor = countingTransactionExecutor();

        final PaymentMethodDispatcher dispatcher = new PaymentMethodDispatcher(
                defaultExecutor,
                ImmutableMap.of(
                        TransactionType.CHARGE, chargeExecutor,
                        TransactionType.REFUND, refundExecutor),
                transactionStateResolver);

        // if only success/failure transactions in the payment - dispatch should not be called
        asList(payments.dummyPaymentAuthSuccess(), payments.dummyPaymentOneChargeSuccess20Euro(),
                payments.dummyPaymentOneAuthFailure20EuroCC(), payments.dummyPaymentOneChargeFailure20Euro())
                .forEach(payment -> {
                    dispatcher.dispatchPayment(new PaymentWithCartLike(payment, null));

                    assertThat(defaultExecutor.getCount())
                            .withFailMessage(format("Default executor is not expected to be called for payment %s", payment.getId()))
                            .isEqualTo(0);

                    assertThat(chargeExecutor.getCount())
                            .withFailMessage(format("Charge executor is not expected to be called for payment %s", payment.getId()))
                            .isEqualTo(0);

                    assertThat(refundExecutor.getCount())
                            .withFailMessage(format("Refund executor is not expected to be called for payment %s", payment.getId()))
                            .isEqualTo(0);
                });
    }
}
