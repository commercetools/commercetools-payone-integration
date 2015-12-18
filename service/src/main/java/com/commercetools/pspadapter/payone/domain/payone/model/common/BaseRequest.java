package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class BaseRequest implements Serializable {

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
    @JsonProperty("api_version")
    private String apiVersion;

    /**
     * Defines request type (preauthorization, authorization, ...)
     */
    private String request;

    public Map<String, Object> toStringMap(final boolean shouldClearSecurityValues) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        if (shouldClearSecurityValues) {
            mapper.addMixIn(this.getClass(), MixIn.class);
        }

        return mapper.convertValue(this,
                mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
    }

    BaseRequest(final PayoneConfig config, final String requestType) {
        this.mid = config.getMerchantId();
        this.key = config.getKeyAsMd5Hash();
        this.mode = config.getMode();
        this.portalid = config.getPortalId();
        this.apiVersion = config.getApiVersion();
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

    public String getApiVersion() {
        return apiVersion;
    }

    //**************************************************************
    //* Filter for Serialization (e.g. clear out security values)
    //**************************************************************

    public interface MixIn {
        @JsonIgnore
        String getKey();
    }

}
