package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.util.ClearSecuredValuesSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class BaseRequest implements Serializable {

    private static final long serialVersionUID = 2L;

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
    @ClearSecuredValuesSerializer.Apply
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
     * {@code ISO 8859-1} or {@code UTF-8}. Default for our implementation is {@code UTF-8},
     * default for Payone is {@code ISO 8859-1}.
     */
    private String encoding;

    /**
     * Payone api version
     */
    @JsonProperty("solution_name")
    private String solutionName;

    /**
     * Payone api version
     */
    @JsonProperty("solution_version")
    private String solutionVersion;

    /**
     * Payone api version
     */
    @JsonProperty("integrator_name")
    private String integratorName;

    /**
     * Payone api version
     */
    @JsonProperty("integrator_version")
    private String integratorVersion;

    /**
     * Defines request type (preauthorization, authorization, ...)
     */
    private String request;

    public Map<String, Object> toStringMap(final boolean shouldClearSecurityValues) {


        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        if (shouldClearSecurityValues) {
            SimpleModule module = new SimpleModule("test", Version.unknownVersion());
            module.addSerializer(String.class, new ClearSecuredValuesSerializer());
            mapper.registerModule(module);
        }

        return mapper.convertValue(this,
                mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
    }

    BaseRequest(final PayoneConfig config, final String requestType) {
        this.mid = config.getMerchantId();
        this.key = config.getKeyAsHash();
        this.mode = config.getMode();
        this.portalid = config.getPortalId();
        this.apiVersion = config.getApiVersion();
        this.encoding = config.getEncoding();
        this.request = requestType;
        this.solutionName = config.getSolutionName();
        this.solutionVersion = config.getSolutionVersion();
        this.integratorName = config.getIntegratorName();
        this.integratorVersion = config.getIntegratorVersion();
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

    public String getEncoding() {
        return encoding;
    }

    public String getSolutionName() {
        return solutionName;
    }

    public String getSolutionVersion() {
        return solutionVersion;
    }

    public String getIntegratorName() {
        return integratorName;
    }

    public String getIntegratorVersion() {
        return integratorVersion;
    }

    //**************************************************************
    //* Filter for Serialization (e.g. clear out security values)
    //**************************************************************

    public interface MixIn {
        @JsonIgnore
        String getKey();

        @JsonIgnore
        String getIban();

        @JsonIgnore
        String getBic();
    }

    static class Views {
        static class Public { }
    }

}
