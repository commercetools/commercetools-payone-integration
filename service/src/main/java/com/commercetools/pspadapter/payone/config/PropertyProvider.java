package com.commercetools.pspadapter.payone.config;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

/**
 * @author fhaertig
 * @date 15.12.15
 */
public class PropertyProvider {

    static final String PAYONE_API_VERSION = "PAYONE_API_VERSION";

    static final String PAYONE_API_URL = "PAYONE_API_URL";
    static final String PAYONE_SUBACC_ID = "PAYONE_SUBACC_ID";
    static final String PAYONE_MERCHANT_ID = "PAYONE_MERCHANT_ID";
    static final String PAYONE_PORTAL_ID = "PAYONE_PORTAL_ID";
    static final String PAYONE_KEY = "PAYONE_KEY";
    static final String PAYONE_MODE = "PAYONE_MODE";

    static final String CT_PROJECT_KEY = "CT_PROJECT_KEY";
    static final String CT_CLIENT_ID = "CT_CLIENT_ID";
    static final String CT_CLIENT_SECRET = "CT_CLIENT_SECRET";

    Map<String, String> internalProperties;

    public PropertyProvider() {
        internalProperties = ImmutableMap.<String, String>builder()
            .put(PAYONE_API_VERSION, "3.9")
            .build();
    }

    public Optional<String> getEnvironmentOrSystemValue(final String propertyName) {
        Optional<String> enviromentValue = Optional.ofNullable(System.getenv(propertyName));
        if (!enviromentValue.isPresent()) {
            //check for system property
            return  Optional.ofNullable(System.getProperty(propertyName));
        }
        return enviromentValue;
    }

    public Map<String, String> getInternalProperties() {
        return internalProperties;
    }


    IllegalArgumentException createIllegalArgumentException(final String propertyName) {
        return new IllegalArgumentException("Value of " + propertyName + " is required and can not be empty!");
    }
}
