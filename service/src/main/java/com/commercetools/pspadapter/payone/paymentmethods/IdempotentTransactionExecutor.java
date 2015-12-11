package com.commercetools.pspadapter.payone.paymentmethods;

import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.types.CustomFields;

import java.util.Optional;

/**
 * Idempotently executes a Transaction of one Type (e.g. Charge) for a specific Payment Method.
 *
 * If no Transaction of that type in State Pending exists, the same Payment is returned.
 */
public interface IdempotentTransactionExecutor extends TransactionExecutor {

    /**
     * @return The Type that is supported.
     */
    TransactionType supportedTransactionType();

    /**
     * Whether the transaction was executed and nothing else can be done by the executor.
     *
     * @param payment
     * @param transaction
     * @return
     */
    boolean wasExecuted(Payment payment, Transaction transaction);

    /**
     * Tries to execute the transaction for the first time.
     * To ensure Idempotency, an InterfaceInteraction is first added to the Payment that can be found later.
     *
     * @param payment
     * @param transaction
     * @return A new version of the Payment. If the attempt has concluded, the state of the Transaction is now either Success or Failure.
     */
    Payment firstExecutionAttempt(Payment payment, Transaction transaction);

    /**
     * Finds a previous execution attempt for the transaction. If there are multiple ones, it selects the last one.
     *
     * @param payment
     * @param transaction
     * @return An attempt, or Optional.empty if there was no previous attempt
     */
    Optional<CustomFields> findLastExecutionAttempt(Payment payment, Transaction transaction);

    /**
     * Retries the last execution attempt for the transaction. It does take necessary precautions for idempotency.
     *
     * @param payment
     * @param transaction
     * @param lastExecutionAttempt
     * @return A new version of the Payment.
     */
    Payment retryLastExecutionAttempt(Payment payment, Transaction transaction, CustomFields lastExecutionAttempt);

    /**
     * Executes a transaction idempotently.
     * @param payment
     * @param transaction
     * @return A new version of the Payment.
     */
    default Payment executeTransaction(Payment payment, Transaction transaction) {
        if (transaction.getType() != supportedTransactionType()) throw new IllegalArgumentException("Unsupported Transaction Type");

        if (wasExecuted(payment, transaction)) return payment;

        return findLastExecutionAttempt(payment, transaction)
            .map(attempt -> retryLastExecutionAttempt(payment, transaction, attempt))
            .orElseGet(() -> firstExecutionAttempt(payment, transaction));
    }
}
