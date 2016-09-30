package com.commercetools.pspadapter.payone.domain.payone.model.common;

public enum ResponseErrorCode {

    /**
     * Error in transaction execution. Means transaction was not successfully posted to Payone service due to some
     * exception (connection, parsing, processing and so on)
     */
    TRANSACTION_EXCEPTION("-1000001");

    private final String errorCode;

    ResponseErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", super.toString(), errorCode);
    }
}
