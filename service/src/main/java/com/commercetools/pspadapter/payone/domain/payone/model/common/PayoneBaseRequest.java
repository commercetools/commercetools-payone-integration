package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.commercetools.pspadapter.payone.PayoneConfig;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.Map;

public class PayoneBaseRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID of the merchant
     */
    private String mid;

    /**
     * ID of payment portal
     */
    private String portalid;

    /**
     * Configurable key of payment portal
     */
    private String key;

    /**
     * test: testmode, live: livemode
     */
    private String mode;

    /**
     * Payone api version
     */
    private String api_version;

    /**
     * Defines request type (preauthorization, authorization, ...). Default will be 'preauthorization'
     */
    private String request;

    public static TypeReference<PayoneBaseRequest> getTypeReference() {
        return new TypeReference<PayoneBaseRequest>() {
            @Override
            public String toString() {
                return "TypeReference<" + PayoneBaseRequest.class.getSimpleName() + ">";
            }
        };
    }

    public Map<String, Object> toStringMap() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.convertValue(this, Map.class);
    }

    PayoneBaseRequest(final PayoneConfig config, final String requestType) {
        this.mid = config.getMerchantId();
        this.key = config.getKey();
        this.mode = config.getMode();
        this.portalid = config.getPortalId();
        this.api_version = config.getApiVersion();
        this.request = requestType;
    }

    //**************************************************************
    //* GETTER AND SETTER METHODS
    //**************************************************************

    public String getMid() {
        return mid;
    }

    public String getPortalid() {
        return portalid;
    }

    public String getKey() {
        return key;
    }

    public String getMode() {
        return mode;
    }

    public String getRequest() {
        return request;
    }

    public String getApi_version() {
        return api_version;
    }
}
