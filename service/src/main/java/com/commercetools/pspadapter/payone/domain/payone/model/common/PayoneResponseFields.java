package com.commercetools.pspadapter.payone.domain.payone.model.common;

public final class PayoneResponseFields {

    // most of requests have it
    public static final String STATUS = "status";


    public static final String REDIRECT = "redirecturl";

    // in case of error response
    public static final String ERROR_CODE = "errorcode";
    public static final String CUSTOMER_MESSAGE = "customermessage";
    public static final String ERROR_MESSAGE = "errormessage";

    public static final String TXID = "txid";

    private PayoneResponseFields() {
    }
}
