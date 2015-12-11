package com.commercetools.pspadapter.payone.domain.payone;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fhaertig
 * @date 11.12.15
 */
public enum RequestType {
    preauthorization("preauthorization"), 
    authorization("authorization"),
    capture("capture"),
    refund("refund"),
    debit("debit"),
    createaccess("createaccess"), 
    vauthorization("vauthorization"),
    getinvoice("getinvoice"),
    updateuser("updateuser"),
    updateaccess("updateaccess"), 
    updatereminder("(updatereminder"),
    creditcardcheck("creditcardcheck"),
    bankaccountcheck("bankaccountcheck"),
    _3dscheck("3dscheck"), 
    addresscheck("addresscheck"),
    consumerscore("consumerscore"),
    callback("callback");

    private final String type;

    private static final Map<String, RequestType> LOOKUP_TYPE = new HashMap<String, RequestType>();

    static {
        for (RequestType p : EnumSet.allOf(RequestType.class)) {
            LOOKUP_TYPE.put(p.getType(), p);
        }
    }

    public static RequestType getRequestType(String type) {
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
