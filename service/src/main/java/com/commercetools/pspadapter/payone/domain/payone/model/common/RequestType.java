package com.commercetools.pspadapter.payone.domain.payone.model.common;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fhaertig
 * @date 11.12.15
 */
public enum RequestType {
    PREAUTHORIZATION("preauthorization"),
    AUTHORIZATION("authorization"),
    CAPTURE("capture"),
    REFUND("refund"),
    DEBIT("debit"),
    GETINVOICE("getinvoice"),
    UPDATEREMINDER("(updatereminder"),
    BANKACCOUNTCHECK("bankaccountcheck"),
    _3DSCHECK("3dscheck"),
    ADDRESSCHECK("addresscheck"),
    CONSUMERSCORE("consumerscore");

    private final String type;

    private static final Map<String, RequestType> LOOKUP_TYPE = new HashMap<String, RequestType>();

    static {
        for (RequestType p : EnumSet.allOf(RequestType.class)) {
            LOOKUP_TYPE.put(p.getType(), p);
        }
    }

    public static RequestType getRequestTypeOf(String type) {
        RequestType requestType = LOOKUP_TYPE.get(type);
        return requestType;
    }

    private RequestType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
