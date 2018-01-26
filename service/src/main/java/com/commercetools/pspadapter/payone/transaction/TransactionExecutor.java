package com.commercetools.pspadapter.payone.transaction;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import io.sphere.sdk.payments.Transaction;

import javax.annotation.Nonnull;

/**
 * Interface for executing pending transactions, i.e. triggering the PSP.
 */
public interface TransactionExecutor {

    /**
     *
     * @param paymentWithCartLike the payment and cartlike to process a transaction
     * @param transaction the transaction to be triggered on the PSP
     * @return the updated version of the payment after triggering the transaction
     */
    @Nonnull
    PaymentWithCartLike executeTransaction(@Nonnull PaymentWithCartLike paymentWithCartLike, @Nonnull Transaction transaction);
}
