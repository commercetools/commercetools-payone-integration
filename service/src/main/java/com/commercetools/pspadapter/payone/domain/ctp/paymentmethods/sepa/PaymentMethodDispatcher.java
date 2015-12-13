package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.sepa;

import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.TransactionExecutor;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;

import java.util.Map;

public class PaymentMethodDispatcher {
    private final TransactionExecutor defaultExecutor;
    private final Map<TransactionType, TransactionExecutor> executorMap;

    public PaymentMethodDispatcher(TransactionExecutor defaultExecutor, Map<TransactionType, TransactionExecutor> executorMap) {
        this.defaultExecutor = defaultExecutor;
        this.executorMap = executorMap;
    }

    public Payment dispatchPayment(Payment payment) {
        // Execute the first Pending Transaction
        return payment
            .getTransactions()
            .stream()
            .filter(transaction -> transaction.getState().equals(TransactionState.PENDING))
            .findFirst()
            .map(transaction -> {
                Payment newPayment = getExecutor(transaction).executeTransaction(payment, transaction);
                Transaction newTransaction = getUpdatedTransaction(transaction, newPayment);
                if (newTransaction.getState().equals(TransactionState.PENDING)) return newPayment; // Still Pending, stop
                else return dispatchPayment(newPayment); // Recursively execute next Pending Transaction
            })
            .orElse(payment);
    }

    private Transaction getUpdatedTransaction(Transaction transaction, Payment newPayment) {
        return newPayment
            .getTransactions()
            .stream()
            .filter(t -> t.getId().equals(transaction.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Transaction can not be found in payment"));
    }

    private TransactionExecutor getExecutor(Transaction transaction) {
        return executorMap.getOrDefault(transaction.getType(), defaultExecutor);
    }
}
