package com.commercetools.pspadapter.payone.paymentmethods;

import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;

public interface TransactionExecutor {
    Payment executeTransaction(Payment payment, Transaction transaction);
}
