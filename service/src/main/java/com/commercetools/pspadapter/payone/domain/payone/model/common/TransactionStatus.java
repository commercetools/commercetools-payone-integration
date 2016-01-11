package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.fasterxml.jackson.annotation.JsonValue;
import io.sphere.sdk.payments.TransactionState;

/**
 * @author fhaertig
 * @date 18.12.15
 */
public enum TransactionStatus {

    PENDING("pending", TransactionState.PENDING),
    COMPLETED("completed", TransactionState.SUCCESS);

    private String payoneCode;
    private TransactionState ctTransactionState;

    TransactionStatus(final String payoneCode, final TransactionState ctTransactionState) {
        this.payoneCode = payoneCode;
        this.ctTransactionState = ctTransactionState;
    }

    @JsonValue
    public String getPayoneCode() {
        return payoneCode;
    }

    public TransactionState getCtTransactionState() {
        return ctTransactionState;
    }
}
