package com.commercetools.pspadapter.payone.domain.payone.model.common;

public final class PayoneResponseFields {

    // most of requests have it
    public static final String STATUS = "status";

    public static final String REDIRECT_URL = "redirecturl";

    // in case of error response
    public static final String ERROR_CODE = "errorcode";
    public static final String CUSTOMER_MESSAGE = "customermessage";
    public static final String ERROR_MESSAGE = "errormessage";

    public static final String TXID = "txid";

    // bank transfer fields
    public static final String BIC = "clearing_bankbic";
    public static final String IBAN = "clearing_bankiban";
    public static final String ACCOUNT_HOLDER = "clearing_bankaccountholder";

    private PayoneResponseFields() {
    }
}
