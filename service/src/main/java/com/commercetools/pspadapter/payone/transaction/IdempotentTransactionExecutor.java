package com.commercetools.pspadapter.payone.transaction;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.cache.LoadingCache;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Idempotently executes a Transaction of one Type (e.g. Charge) for a specific PaymentWithCartLike Method.
 *
 * If no Transaction of that type in State Pending exists, the same PaymentWithCartLike is returned.
 */
public abstract class IdempotentTransactionExecutor implements TransactionExecutor {

    private LoadingCache<String, Type> typeCache;

    public IdempotentTransactionExecutor(@Nonnull final LoadingCache<String, Type> typeCache) {
        this.typeCache = typeCache;
    }

    /**
     * @return The Type that is supported.
     */
    public abstract TransactionType supportedTransactionType();

    /**
     * Executes a transaction idempotently.
     * @param paymentWithCartLike
     * @param transaction
     * @return A new version of the PaymentWithCartLike.
     */
    @Override
    public PaymentWithCartLike executeTransaction(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
        if (transaction.getType() != supportedTransactionType()) throw new IllegalArgumentException("Unsupported Transaction Type");

        if (wasExecuted(paymentWithCartLike, transaction)) return paymentWithCartLike;

        return findLastExecutionAttempt(paymentWithCartLike, transaction)
                .map(attempt -> retryLastExecutionAttempt(paymentWithCartLike, transaction, attempt))
                .orElseGet(() -> attemptFirstExecution(paymentWithCartLike, transaction));
    }

    /**
     * Whether the transaction was executed and nothing else can be done by the executor.
     *
     * @param paymentWithCartLike
     * @param transaction
     * @return
     */
    protected abstract boolean wasExecuted(PaymentWithCartLike paymentWithCartLike, Transaction transaction);
    /**
     * Tries to execute the transaction for the first time.
     * To ensure Idempotency, an InterfaceInteraction is first added to the PaymentWithCartLike that can be found later.
     *
     * @param paymentWithCartLike
     * @param transaction
     * @return A new version of the PaymentWithCartLike. If the attempt has concluded, the state of the Transaction is now either Success or Failure.
     */
    protected abstract PaymentWithCartLike attemptFirstExecution(PaymentWithCartLike paymentWithCartLike, Transaction transaction);

    /**
     * Finds a previous execution attempt for the transaction. If there are multiple ones, it selects the last one.
     *
     * @param paymentWithCartLike
     * @param transaction
     * @return An attempt, or Optional.empty if there was no previous attempt
     */
    protected abstract Optional<CustomFields> findLastExecutionAttempt(PaymentWithCartLike paymentWithCartLike, Transaction transaction);

    /**
     * Retries the last execution attempt for the transaction. It does take necessary precautions for idempotency.
     *
     * @param paymentWithCartLike
     * @param transaction
     * @param lastExecutionAttempt
     * @return A new version of the PaymentWithCartLike.
     */
    protected abstract PaymentWithCartLike retryLastExecutionAttempt(PaymentWithCartLike paymentWithCartLike, Transaction transaction, CustomFields lastExecutionAttempt);

    /**
     * Determines the next sequence number to use from already received notifications.
     *
     * @param paymentWithCartLike the payment with cart/order to search in
     * @return 0 if no notifications received yet, else the highest sequence number received + 1
     */
    protected int getNextSequenceNumber(final PaymentWithCartLike paymentWithCartLike) {
        IntUnaryOperator increase = (x) -> x + 1;
        Predicate<String> isInteger = (i) -> i != null && i.matches("-?[0-9]+");

        return IntStream.concat(
            getCustomFieldsOfType(paymentWithCartLike, CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION)
                .map(f -> f.getFieldAsString(CustomFieldKeys.SEQUENCE_NUMBER_FIELD))
                .filter(isInteger::test)
                .mapToInt(Integer::parseInt),
            paymentWithCartLike
                .getPayment()
                .getTransactions()
                .stream()
                .filter(t -> isInteger.test(t.getInteractionId()))
                .mapToInt(t -> Integer.parseInt(t.getInteractionId().trim()))
        )
        .map(increase)
        .max()
        .orElse(0);
    }

    protected Stream<CustomFields> getCustomFieldsOfType(PaymentWithCartLike paymentWithCartLike, String... typeKeys) {
        return paymentWithCartLike
                .getPayment()
                .getInterfaceInteractions()
                .stream()
                .filter(i -> Arrays.stream(typeKeys)
                        .map(t -> getTypeCache().getUnchecked(t).toReference())
                        .anyMatch(t -> t.getId().equals(i.getType().getId())));
    }



    private LoadingCache<String, Type> getTypeCache() {
        return this.typeCache;
    }
}
