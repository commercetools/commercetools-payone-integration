package com.commercetools.pspadapter.payone.util;

import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public final class PaymentUtil {

    public static Optional<Transaction> getTransactionById(@Nullable Payment payment, @Nullable String transactionId) {
        return ofNullable(payment)
                .map(Payment::getTransactions)
                .map(List::stream)
                .flatMap(transactions -> transactions
                        .filter(tr -> tr.getId().equals(transactionId))
                        .findFirst());
    }

    private PaymentUtil() {
    }
}
