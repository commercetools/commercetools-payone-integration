package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * @author fhaertig
 * @date 17.12.15
 */
public enum NotificationAction implements Serializable {

    APPOINTED("appointed"),
    CAPTURE("capture"),
    PAID("paid"),
    UNDERPAID("underpaid"),
    CANCELATION("cancelation"),
    REFUND("refund"),
    DEBIT("debit"),
    REMINDER("reminder"),
    TRANSFER("transfer"),
    INVOICE("invoice"),
    FAILED("failed");

    private String txActionCode;

    NotificationAction(final String txActionCode) {
        this.txActionCode = txActionCode;
    }

    @JsonValue
    public String getTxActionCode() {
        return txActionCode;
    }
}
