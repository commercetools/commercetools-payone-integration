package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.fasterxml.jackson.annotation.JsonValue;
import io.sphere.sdk.payments.TransactionState;

/**
 * Enumeration of possible PAYONE transaction status values
 * (sent by PAYONE's POST requests for transaction status notification events).
 *
 * @author fhaertig
 * @author Jan Wolter
 * @since 18.12.15
 */
public enum TransactionStatus {

    PENDING("pending", TransactionState.PENDING),
    COMPLETED("completed", TransactionState.SUCCESS);

    private final String payoneCode;
    private final TransactionState ctTransactionState;

    TransactionStatus(final String payoneCode, final TransactionState ctTransactionState) {
        this.payoneCode = payoneCode;
        this.ctTransactionState = ctTransactionState;
    }

    @JsonValue
    public String getPayoneCode() {
        return payoneCode;
    }

    /**
     * Gets the commercetools {@link TransactionState} mapped to the PAYONE status.
     * @return the commercetools TransactionState
     */
    public TransactionState getCtTransactionState() {
        return ctTransactionState;
    }
}
