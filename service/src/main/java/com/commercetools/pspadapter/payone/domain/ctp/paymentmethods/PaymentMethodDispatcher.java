package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;

public class PaymentMethodDispatcher {
    private final TransactionExecutor defaultExecutor;
    private final ImmutableMap<TransactionType, TransactionExecutor> executors;

    public PaymentMethodDispatcher(
            final TransactionExecutor defaultExecutor,
            final ImmutableMap<TransactionType, TransactionExecutor> executors) {
        this.defaultExecutor = defaultExecutor;
        this.executors = executors;
    }

    public PaymentWithCartLike dispatchPayment(final PaymentWithCartLike paymentWithCartLike) {
        // Execute the first Pending Transaction
        return paymentWithCartLike.getPayment()
            .getTransactions()
            .stream()
            .filter(transaction -> TransactionState.PENDING.equals(transaction.getState()))
            .findFirst()
            .map(transaction -> {
                final PaymentWithCartLike newPaymentWithCartLike = executeTransaction(paymentWithCartLike, transaction);
                final Transaction updatedTransaction = getUpdatedTransaction(transaction, newPaymentWithCartLike);

                if (TransactionState.PENDING.equals(updatedTransaction.getState())) { // Still Pending, stop ->
                    // executor has done its duty, notification from Payone required to proceed with this transaction
                    return newPaymentWithCartLike;
                } else { // Recursively execute next Pending Transaction
                    return dispatchPayment(newPaymentWithCartLike);
                }
            })
            .orElse(paymentWithCartLike);
    }

    private PaymentWithCartLike executeTransaction(final PaymentWithCartLike paymentWithCartLike,
                                                   final Transaction transaction) {
        return executors.getOrDefault(transaction.getType(), defaultExecutor)
                .executeTransaction(paymentWithCartLike, transaction);
    }

    private static Transaction getUpdatedTransaction(final Transaction transaction,
                                                     final PaymentWithCartLike paymentWithCartLike) {
        return paymentWithCartLike.getPayment()
            .getTransactions()
            .stream()
            .filter(t -> t.getId().equals(transaction.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Transaction can not be found in payment"));
    }

}