package com.commercetools.pspadapter.payone.config;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

/**
 * @author fhaertig
 * @date 15.12.15
 */
public class PropertyProvider {

    public static final String PAYONE_API_VERSION = "PAYONE_API_VERSION";

    public static final String PAYONE_API_URL = "PAYONE_API_URL";
    public static final String PAYONE_SUBACC_ID = "PAYONE_SUBACC_ID";
    public static final String PAYONE_MERCHANT_ID = "PAYONE_MERCHANT_ID";
    public static final String PAYONE_PORTAL_ID = "PAYONE_PORTAL_ID";
    public static final String PAYONE_KEY = "PAYONE_KEY";
    public static final String PAYONE_MODE = "PAYONE_MODE";

    public static final String CT_PROJECT_KEY = "CT_PROJECT_KEY";
    public static final String CT_CLIENT_ID = "CT_CLIENT_ID";
    public static final String CT_CLIENT_SECRET = "CT_CLIENT_SECRET";
    public static final String CT_START_FROM_SCRATCH = "CT_START_FROM_SCRATCH";

    Map<String, String> internalProperties;

    public PropertyProvider() {
        internalProperties = ImmutableMap.<String, String>builder()
            .put(PAYONE_API_VERSION, "3.9")
            .build();
    }

    /**
     * Gets an optional property.
     * @param propertyName the name of the requested property, must not be null
     * @return the property, an empty Optional if not present; empty values are treated as present
     */
    public Optional<String> getProperty(final String propertyName) {
        final Optional<String> internalProperty = Optional.ofNullable(internalProperties.get(propertyName));
        if (internalProperty.isPresent()) {
            return internalProperty;
        }

        final Optional<String> environmentValue = Optional.ofNullable(System.getenv(propertyName));
        if (environmentValue.isPresent()) {
            return environmentValue;
        }
        return Optional.ofNullable(System.getProperty(propertyName));
    }

    /**
     * Gets a mandatory non-empty property.
     * @param propertyName the name of the requested property, must not be null
     * @return the property value
     * @throws IllegalStateException if the property isn't defined or empty
     */
    public String getMandatoryNonEmptyProperty(final String propertyName) {
        return getProperty(propertyName)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> createIllegalStateException(propertyName));
    }

    private IllegalStateException createIllegalStateException(final String propertyName) {
        return new IllegalStateException("Value of " + propertyName + " is required and can not be empty!");
    }
}
