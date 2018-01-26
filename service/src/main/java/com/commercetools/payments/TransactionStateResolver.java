package com.commercetools.payments;

import io.sphere.sdk.payments.Transaction;

import javax.annotation.Nonnull;

/**
 * This interface is implemented to support migrating from default
 * {@link io.sphere.sdk.payments.TransactionState#PENDING} to default
 * {@link io.sphere.sdk.payments.TransactionState#INITIAL}.
 * <p>
 * TODO: When full platform and customers migration is done - we should simplify or remove resolver method
 * and accept only <i>Initial</i> state as unprocessed, fixing respective tests and test mocks.
 *
 * @see <a href="http://dev.commercetools.com/release-notes.html#release-notes---commercetools-platform---version-release-29-september-2017">
 * Release Notes - commercetools platform - Version Release 29 September 2017</a>
 */
public interface TransactionStateResolver {

    /**
     * Verify that transaction is still expected to be in initial phase, e.g. not processed by payment provider.
     * During migration mode this "initial" phase expected to be {@link io.sphere.sdk.payments.TransactionState#PENDING}
     * or {@link io.sphere.sdk.payments.TransactionState#INITIAL}.
     *
     * TODO: this method should be re-factored when full migration to <i>Initial</i> state is done. Search for
     * "Initial/PendingFix" marker string in the sources to find out which tests/methods should be re-factored.
     *
     * @param transaction transaction to verify
     * @return <b>true</b> if the transaction is in pending or initial state.
     */
    boolean isNotCompletedTransaction(@Nonnull Transaction transaction);
}
