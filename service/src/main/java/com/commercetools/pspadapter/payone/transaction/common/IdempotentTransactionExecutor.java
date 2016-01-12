package com.commercetools.pspadapter.payone.transaction.common;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.types.CustomFields;

import java.util.Optional;

/**
 * Idempotently executes a Transaction of one Type (e.g. Charge) for a specific PaymentWithCartLike Method.
 *
 * If no Transaction of that type in State Pending exists, the same PaymentWithCartLike is returned.
 */
public interface IdempotentTransactionExecutor extends TransactionExecutor {

    /**
     * @return The Type that is supported.
     */
    TransactionType supportedTransactionType();

    /**
     * Whether the transaction was executed and nothing else can be done by the executor.
     *
     * @param paymentWithCartLike
     * @param transaction
     * @return
     */
    boolean wasExecuted(PaymentWithCartLike paymentWithCartLike, Transaction transaction);

    /**
     * Tries to execute the transaction for the first time.
     * To ensure Idempotency, an InterfaceInteraction is first added to the PaymentWithCartLike that can be found later.
     *
     * @param paymentWithCartLike
     * @param transaction
     * @return A new version of the PaymentWithCartLike. If the attempt has concluded, the state of the Transaction is now either Success or Failure.
     */
    PaymentWithCartLike attemptFirstExecution(PaymentWithCartLike paymentWithCartLike, Transaction transaction);

    /**
     * Finds a previous execution attempt for the transaction. If there are multiple ones, it selects the last one.
     *
     * @param paymentWithCartLike
     * @param transaction
     * @return An attempt, or Optional.empty if there was no previous attempt
     */
    Optional<CustomFields> findLastExecutionAttempt(PaymentWithCartLike paymentWithCartLike, Transaction transaction);

    /**
     * Retries the last execution attempt for the transaction. It does take necessary precautions for idempotency.
     *
     * @param paymentWithCartLike
     * @param transaction
     * @param lastExecutionAttempt
     * @return A new version of the PaymentWithCartLike.
     */
    PaymentWithCartLike retryLastExecutionAttempt(PaymentWithCartLike paymentWithCartLike, Transaction transaction, CustomFields lastExecutionAttempt);

    /**
     * Executes a transaction idempotently.
     * @param paymentWithCartLike
     * @param transaction
     * @return A new version of the PaymentWithCartLike.
     */
    default PaymentWithCartLike executeTransaction(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
        if (transaction.getType() != supportedTransactionType()) throw new IllegalArgumentException("Unsupported Transaction Type");

        if (wasExecuted(paymentWithCartLike, transaction)) return paymentWithCartLike;

        return findLastExecutionAttempt(paymentWithCartLike, transaction)
            .map(attempt -> retryLastExecutionAttempt(paymentWithCartLike, transaction, attempt))
            .orElseGet(() -> attemptFirstExecution(paymentWithCartLike, transaction));
    }
}
