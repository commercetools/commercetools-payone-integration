package com.commercetools.payments;

import io.sphere.sdk.payments.Transaction;

import javax.annotation.Nonnull;

import static io.sphere.sdk.payments.TransactionState.INITIAL;
import static io.sphere.sdk.payments.TransactionState.PENDING;

public class TransactionStateResolverImpl implements TransactionStateResolver {

    /**
     * @inheritDoc
     */
    @Override
    public boolean isNotCompletedTransaction(@Nonnull final Transaction transaction) {
        return transaction.getState() == INITIAL || transaction.getState() == PENDING;
    }
}
