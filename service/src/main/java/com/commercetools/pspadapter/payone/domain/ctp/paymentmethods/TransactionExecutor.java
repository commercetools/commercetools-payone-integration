package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;

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
    PaymentWithCartLike executeTransaction(PaymentWithCartLike paymentWithCartLike, Transaction transaction);
}
