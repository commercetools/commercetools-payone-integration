package com.commercetools.pspadapter.payone.domain.payone.model;

import com.commercetools.pspadapter.payone.PayoneConfig;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.Serializable;
import java.util.List;

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
     * Payone api version
     */
    private String api_version;

    /**
     * Defines request type (preauthorization, authorization, ...). Default will be 'preauthorization'
     */
    private String request;

    /**
     * Defines a shippingprovider required for the paymentMode "PayoneCOD". Default will be 'DHL'.
     */
    private String shippingProvider;

    /**
     * List of country codes where 3D-secure check for creditcards is excluded
     */
    private List<String> exclude3DSecureCountries;

    public static TypeReference<PayoneBaseRequest> getTypeReference() {
        return new TypeReference<PayoneBaseRequest>() {
            @Override
            public String toString() {
                return "TypeReference<" + PayoneBaseRequest.class.getSimpleName() + ">";
            }
        };
    }

    private PayoneBaseRequest() {

    }


    /**
     * PayoneBaseRequest builder with fluent api.
     */
    public static class PayoneRequestBuilder {

        private final PayoneBaseRequest built;

        /**
         * Builder constructor, using only the name of the product to be created here.
         *
         * @param config The PayoneConfig to use for the new @PayoneBaseRequest.
         */
        public PayoneRequestBuilder(final PayoneConfig config, final String requestType) {
            this.built = new PayoneBaseRequest();

            this.built.aid = config.getSubAccountId();
            this.built.mid = config.getMerchantId();
            this.built.key = config.getKey();
            this.built.mode = config.getMode();
            this.built.portalid = config.getPortalId();
            this.built.request = requestType;
        }

        /**
         * Starts fluent interface style object creation.
         *
         * @return The @PayoneBaseRequest instance created by this @Builder.
         */
        public PayoneBaseRequest build() {
            return this.built;
        }

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

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
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
