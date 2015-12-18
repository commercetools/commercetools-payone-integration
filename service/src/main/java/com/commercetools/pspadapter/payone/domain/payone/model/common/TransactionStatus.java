package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author fhaertig
 * @date 18.12.15
 */
public enum TransactionStatus {

    PENDING("pending"),
    COMPLETED("completed");

    private String payoneCode;

    TransactionStatus(final String payoneCode) {
        this.payoneCode = payoneCode;
    }

    @JsonValue
    public String getPayoneCode() {
        return payoneCode;
    }
}
