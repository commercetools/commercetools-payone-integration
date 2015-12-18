package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods;

import com.google.common.collect.ImmutableSet;
import io.sphere.sdk.payments.TransactionType;

import java.util.HashSet;
import java.util.Set;

public enum PaymentMethod {

    DIRECT_DEBIT_SEPA(MethodKeys.DIRECT_DEBIT_SEPA,
            ImmutableSet.of(
                    TransactionType.AUTHORIZATION)
    ),

    CREDIT_CARD(MethodKeys.CREDIT_CARD,
            ImmutableSet.of(
                    TransactionType.AUTHORIZATION,
                    TransactionType.CHARGE)
    );

    private String key;
    private Set<TransactionType> supportedTransactionTypes;

    PaymentMethod(final String key) {
        this(key, new HashSet<>());
    }

    PaymentMethod(final String key, final Set<TransactionType> supportedTransactionTypes) {
        this.key = key;
        this.supportedTransactionTypes = supportedTransactionTypes;
    }

    public String getKey() {
        return key;
    }

    public Set<TransactionType> getSupportedTransactionTypes() {
        return supportedTransactionTypes;
    }

    public static PaymentMethod getMethodFromString(final String methodString) {
        try {
            return PaymentMethod.valueOf(methodString);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
