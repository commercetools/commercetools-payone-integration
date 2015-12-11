package com.commercetools.pspadapter.payone.domain.payone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;

public class PayoneRequest implements Serializable {

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
     * ID of the sub account
     */
    private String aid;

    /**
     * Configurable key of payment portal
     */
    private String key;

    /**
     * test: testmode, live: livemode
     */
    private String mode;

    /**
     * Url entry point to the payone Server-API
     */
    private String payoneServerApiUrl;

    /**
     * Defines request type (preauthorization, authorization, ...). Default will be 'preauthorization'
     */
    private String requestType;

    /**
     * Defines a shippingprovider required for the paymentMode "PayoneCOD". Default will be 'DHL'.
     */
    private String shippingProvider;

    /**
     * List of country codes where 3D-secure check for creditcards is excluded
     */
    private List<String> exclude3DSecureCountries;

    @JsonCreator
    public PayoneRequest(@JsonProperty("mid") String mid, @JsonProperty("portalid") String portalid, @JsonProperty("aid") String aid,
                         @JsonProperty("key") String key, @JsonProperty("mode") String mode, @JsonProperty("payoneServerApiUrl") String payoneServerApiUrl,
                         @JsonProperty("requestType") String requestType, @JsonProperty("shippingProvider") String shippingProvider,
                         @JsonProperty("exlude3DSecureCountries") List<String> exlude3DSecureCountries) {
        this.mid = mid;
        this.portalid = portalid;
        this.aid = aid;
        this.key = key;
        this.mode = mode;
        this.payoneServerApiUrl = payoneServerApiUrl;
        this.requestType = StringUtils.isNotBlank(requestType) ? requestType : RequestType.preauthorization.getType();
        //TODO: Default shipping provider from custom properties
        this.shippingProvider = StringUtils.isNotBlank(shippingProvider) ? shippingProvider : "";
        this.exclude3DSecureCountries = exlude3DSecureCountries;
    }

    public static PayoneRequest of(final String mid, final String portalid, final String aid, final String key, final String mode,
            final String payoneServerApiUrl, final String requestType, final String shippingProvider, final List<String> exclude3DSecureCountries) {
        return new PayoneRequest(mid, portalid, aid, key, mode, payoneServerApiUrl, requestType, shippingProvider, exclude3DSecureCountries);
    }

    public static TypeReference<PayoneRequest> getTypeReference() {
        return new TypeReference<PayoneRequest>() {
            @Override
            public String toString() {
                return "TypeReference<" + PayoneRequest.class.getSimpleName() + ">";
            }
        };
    }

    //**************************************************************
    //* GETTER AND SETTER METHODS
    //**************************************************************

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public String getPortalid() {
        return portalid;
    }

    public void setPortalid(String portalid) {
        this.portalid = portalid;
    }

    public String getAid() {
        return aid;
    }

    public void setAid(String aid) {
        this.aid = aid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getPayoneServerApiUrl() {
        return payoneServerApiUrl;
    }

    public void setPayoneServerApiUrl(String payoneServerApiUrl) {
        this.payoneServerApiUrl = payoneServerApiUrl;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getShippingProvider() {
        return shippingProvider;
    }

    public void setShippingProvider(String shippingProvider) {
        this.shippingProvider = shippingProvider;
    }

    public List<String> getExclude3DSecureCountries() {
        return exclude3DSecureCountries;
    }

    public void setExclude3DSecureCountries(List<String> exclude3dSecureCountries) {
        exclude3DSecureCountries = exclude3dSecureCountries;
    }

}
